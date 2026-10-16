package com.arhs.tools.ai.service;

import com.arhs.tools.ai.domain.*;
import com.arhs.tools.ai.dto.DuplicateDtos.*;
import com.arhs.tools.ai.exception.RegistryConflictException;
import com.arhs.tools.ai.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/** Actual competing H2 transactions, with barriers before writes rather than
 * simulated lock exceptions. All worker transactions have bounded waits. */
@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:duplicates-concurrency;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "spring.jpa.hibernate.ddl-auto=create-drop"})
class DuplicateConcurrencyIntegrationTest {
    @Autowired CivilRegistryRepository persons;
    @Autowired DuplicateCaseRepository cases;
    @Autowired DuplicateCaseEventRepository events;
    @Autowired PersonMergeAuditRepository audits;
    @Autowired DuplicateManagementService service;
    @Autowired CivilRegistryService civil;
    @Autowired PlatformTransactionManager manager;
    @SpyBean DuplicateMatcher matcher;
    private ExecutorService workers;
    private TransactionTemplate tx;
    @BeforeEach void setup() {
        reset(matcher); events.deleteAll(); audits.deleteAll(); cases.deleteAll(); persons.deleteAll();
        workers = Executors.newFixedThreadPool(2); tx = new TransactionTemplate(manager); tx.setTimeout(20);
    }
    @AfterEach void stop() { workers.shutdownNow(); reset(matcher); }
    private Person person(String username) {
        return persons.saveAndFlush(new Person(username, "First", "Last", username + "@example.test", LocalDate.of(1990, 1, 1)));
    }
    private CaseDetail pair() { person("one"); person("two"); service.scan(); return service.detail(cases.findAll().get(0).getId()); }
    private MergeRequest request(CaseDetail d) {
        Map<String, Integer> sources = new HashMap<>(); DuplicateManagementService.FIELDS.forEach(field -> sources.put(field, d.rightPerson().id()));
        return new MergeRequest(d.duplicateCase().version(), d.leftPerson().id(),
                Map.of(d.leftPerson().id(), d.leftPerson().version(), d.rightPerson().id(), d.rightPerson().version()), sources, "Verified matching records", null);
    }
    private void await(CountDownLatch latch) {
        try { if (!latch.await(15, TimeUnit.SECONDS)) throw new AssertionError("Transaction barrier timed out"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
    }
    private void await(CyclicBarrier barrier) {
        try { barrier.await(15, TimeUnit.SECONDS); }
        catch (Exception e) { throw new AssertionError(e); }
    }
    private Throwable outcome(Runnable action) {
        try { action.run(); return null; } catch (Throwable e) { return e; }
    }
    @Test void concurrentDismissalsCommitExactlyOneResolutionAndEvent() throws Exception {
        CaseDetail d = pair(); CyclicBarrier loaded = new CyclicBarrier(2);
        Callable<Throwable> dismiss = () -> outcome(() -> tx.execute(ignored -> {
            cases.findById(d.duplicateCase().id()).orElseThrow(); await(loaded);
            return service.dismiss(d.duplicateCase().id(), new DismissRequest(d.duplicateCase().version(), "Confirmed separate identities", null));
        }));
        Future<Throwable> one = workers.submit(dismiss), two = workers.submit(dismiss);
        List<Throwable> results = Arrays.asList(one.get(20, TimeUnit.SECONDS), two.get(20, TimeUnit.SECONDS));
        assertThat(results.stream().filter(Objects::isNull).count()).isEqualTo(1);
        assertThat(results.stream().filter(Objects::nonNull).findFirst().orElseThrow()).isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(events.count()).isEqualTo(2); assertThat(audits.count()).isZero();
        assertThat(service.detail(d.duplicateCase().id()).duplicateCase().status()).isEqualTo(DuplicateCase.Status.DISMISSED);
    }
    @Test void editDuringMergeRollsBackRetirementCaseAndAudit() throws Exception {
        CaseDetail d = pair(); CountDownLatch loaded = new CountDownLatch(1), edited = new CountDownLatch(1);
        Future<Throwable> merge = workers.submit(() -> outcome(() -> tx.execute(ignored -> {
            cases.findById(d.duplicateCase().id()).orElseThrow();
            persons.findById(d.leftPerson().id()).orElseThrow(); persons.findById(d.rightPerson().id()).orElseThrow();
            loaded.countDown(); await(edited); return service.merge(d.duplicateCase().id(), request(d));
        })));
        await(loaded);
        try {
            Person edit = persons.findById(d.leftPerson().id()).orElseThrow(); edit.setFirstName("Concurrent edit"); civil.update(edit);
        } finally { edited.countDown(); }
        assertThat(merge.get(20, TimeUnit.SECONDS)).isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(persons.findById(d.leftPerson().id()).orElseThrow().getFirstName()).isEqualTo("Concurrent edit");
        Person retired = persons.findById(d.rightPerson().id()).orElseThrow();
        assertThat(retired.getStatus()).isEqualTo(Person.Status.ACTIVE); assertThat(retired.getUserName()).isEqualTo("two");
        assertThat(audits.count()).isZero(); assertThat(events.count()).isEqualTo(1);
        assertThat(service.detail(d.duplicateCase().id()).duplicateCase().status()).isEqualTo(DuplicateCase.Status.OPEN);
    }
    @Test void thirdPartyTakingReleasedSourceIdentifierBlocksStaleMergeWithoutPartialChanges() throws Exception {
        CaseDetail d = pair(); CountDownLatch loaded = new CountDownLatch(1), transferred = new CountDownLatch(1);
        Future<Throwable> merge = workers.submit(() -> outcome(() -> tx.execute(ignored -> {
            persons.findById(d.leftPerson().id()).orElseThrow(); persons.findById(d.rightPerson().id()).orElseThrow();
            cases.findById(d.duplicateCase().id()).orElseThrow(); loaded.countDown(); await(transferred);
            return service.merge(d.duplicateCase().id(), request(d));
        })));
        await(loaded);
        try {
            Person edit = persons.findById(d.rightPerson().id()).orElseThrow(); edit.setUserName("renamed"); civil.update(edit);
            persons.saveAndFlush(new Person("two", "Third", "Party", "third@example.test", LocalDate.of(1980, 1, 1)));
        } finally { transferred.countDown(); }
        assertThat(merge.get(20, TimeUnit.SECONDS)).isInstanceOf(RegistryConflictException.class);
        assertThat(persons.findById(d.leftPerson().id()).orElseThrow().getUserName()).isEqualTo("one");
        assertThat(persons.findById(d.rightPerson().id()).orElseThrow().getStatus()).isEqualTo(Person.Status.ACTIVE);
        assertThat(audits.count()).isZero(); assertThat(events.count()).isEqualTo(1);
    }
    @Test void concurrentOrdinaryEditsUseOptimisticLocking() throws Exception {
        Person p = person("one"); CyclicBarrier loaded = new CyclicBarrier(2);
        Callable<Throwable> update = () -> outcome(() -> tx.execute(ignored -> {
            Person current = persons.findById(p.getId()).orElseThrow();
            Person edit = new Person(current); edit.setId(current.getId()); edit.setFirstName(Thread.currentThread().getName());
            await(loaded); return civil.update(edit);
        }));
        Future<Throwable> one = workers.submit(update), two = workers.submit(update);
        List<Throwable> results = Arrays.asList(one.get(20, TimeUnit.SECONDS), two.get(20, TimeUnit.SECONDS));
        assertThat(results.stream().filter(Objects::isNull).count()).isEqualTo(1);
        assertThat(results.stream().filter(Objects::nonNull).findFirst().orElseThrow()).isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
    @Test void competingIdentifierClaimsKeepDatabaseConstraintAndRollBackLoser() throws Exception {
        CyclicBarrier ready = new CyclicBarrier(2);
        Callable<Throwable> create = () -> outcome(() -> tx.execute(ignored -> {
            String thread = Thread.currentThread().getName();
            persons.saveAndFlush(new Person(thread, "Different", "Person", thread + "@example.test", LocalDate.of(1990, 1, 1)));
            await(ready);
            return persons.saveAndFlush(new Person("shared", "First", "Last", thread + "-shared@example.test", LocalDate.of(1990, 1, 1)));
        }));
        Future<Throwable> one = workers.submit(create), two = workers.submit(create);
        List<Throwable> results = Arrays.asList(one.get(20, TimeUnit.SECONDS), two.get(20, TimeUnit.SECONDS));
        assertThat(results.stream().filter(Objects::isNull).count()).isEqualTo(1);
        assertThat(results.stream().filter(Objects::nonNull).findFirst().orElseThrow()).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(persons.count()).isEqualTo(2);
    }
    @Test void overlappingScansConflictAndGuardIsReleasedAfterFailure() throws Exception {
        person("one"); CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        doAnswer(invocation -> { started.countDown(); await(release); return invocation.callRealMethod(); }).when(matcher).candidates(anyList());
        Future<ScanResult> first = workers.submit(service::scan); await(started);
        try { assertThatThrownBy(service::scan).isInstanceOf(RegistryConflictException.class); }
        finally { release.countDown(); }
        first.get(20, TimeUnit.SECONDS);
        reset(matcher);
        doThrow(new IllegalStateException("Synthetic scan failure")).doCallRealMethod().when(matcher).candidates(anyList());
        assertThatThrownBy(service::scan).isInstanceOf(IllegalStateException.class);
        assertThat(service.scan().activePersons()).isEqualTo(1);
    }
}
