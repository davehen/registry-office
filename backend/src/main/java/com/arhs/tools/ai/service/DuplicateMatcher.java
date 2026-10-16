package com.arhs.tools.ai.service;

import com.arhs.tools.ai.domain.Person;
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;

/** Pure matching and blocking. Never changes or logs a person's fields. */
@Component
public class DuplicateMatcher {
    public record Match(int score, List<String> reasons) { }
    public record Pair(int left, int right) implements Comparable<Pair> {
        public Pair { if (left >= right) throw new IllegalArgumentException("Unordered pair"); }
        @Override public int compareTo(Pair other) {
            int order = Integer.compare(left, other.left);
            return order == 0 ? Integer.compare(right, other.right) : order;
        }
    }
    private record Email(String local, String domain) { }
    private record Key(String rule, List<String> parts) { }

    public String normalizeName(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT).replaceAll("['’.-]", "")
                .replaceAll("[\\p{Z}\\s]+", " ").strip();
    }
    public String normalizeEmail(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }
    private boolean equal(String a, String b) { return !a.isEmpty() && a.equals(b); }
    private String initial(String name) {
        return name.isEmpty() ? "" : new String(Character.toChars(name.codePointAt(0)));
    }
    private Email emailParts(String value) {
        String email = normalizeEmail(value);
        int at = email.indexOf('@');
        if (at <= 0 || at != email.lastIndexOf('@') || at == email.length() - 1
                || email.codePoints().anyMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) return null;
        String local = email.substring(0, at), domain = email.substring(at + 1);
        if (!local.matches("[a-z0-9.!#$%&'*+/=?^_`{|}~-]+") || local.startsWith(".") || local.endsWith(".") || local.contains("..")
                || !domain.matches("[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)*")) return null;
        return new Email(local, domain);
    }

    public Match match(Person a, Person b) {
        String af = normalizeName(a.getFirstName()), bf = normalizeName(b.getFirstName());
        String al = normalizeName(a.getLastName()), bl = normalizeName(b.getLastName());
        boolean fullName = equal(af, bf) && equal(al, bl);
        boolean sameDate = a.getBirthDate() != null && a.getBirthDate().equals(b.getBirthDate());
        List<String> reasons = new ArrayList<>();
        int score = 0;
        if (equal(normalizeEmail(a.getEmail()), normalizeEmail(b.getEmail()))) { reasons.add("EMAIL_EXACT"); score = 100; }
        if (equal(normalizeName(a.getUserName()), normalizeName(b.getUserName()))) { reasons.add("USERNAME_EXACT"); score = 100; }
        if (fullName && sameDate) { reasons.add("FULL_NAME_BIRTH_DATE"); score = Math.max(score, 90); }
        if (equal(al, bl) && equal(initial(af), initial(bf)) && sameDate) {
            reasons.add("LAST_NAME_FIRST_INITIAL_BIRTH_DATE"); score = Math.max(score, 75);
        }
        if (fullName && a.getBirthDate() != null && b.getBirthDate() != null
                && Math.abs(a.getBirthDate().toEpochDay() - b.getBirthDate().toEpochDay()) == 1) {
            reasons.add("FULL_NAME_BIRTH_DATE_ONE_DAY"); score = Math.max(score, 70);
        }
        Email ae = emailParts(a.getEmail()), be = emailParts(b.getEmail());
        if (fullName && ae != null && be != null && ae.local.equals(be.local) && !ae.domain.equals(be.domain)) {
            reasons.add("FULL_NAME_EMAIL_LOCAL_PART"); score = Math.max(score, 65);
        }
        return new Match(score, List.copyOf(reasons));
    }

    public SortedSet<Pair> candidates(List<Person> persons) {
        Map<Key, SortedSet<Integer>> blocks = new HashMap<>();
        for (Person p : persons) {
            if (p.getStatus() != Person.Status.ACTIVE) continue;
            String first = normalizeName(p.getFirstName()), last = normalizeName(p.getLastName());
            add(blocks, p, "email", normalizeEmail(p.getEmail()));
            add(blocks, p, "username", normalizeName(p.getUserName()));
            if (p.getBirthDate() != null) {
                String date = p.getBirthDate().toString();
                add(blocks, p, "full-date", first, last, date);
                add(blocks, p, "initial-date", initial(first), last, date);
                // Overlapping calendar-day buckets cover both directions once.
                add(blocks, p, "adjacent-date", first, last, date);
                if (!p.getBirthDate().equals(LocalDate.MAX))
                    add(blocks, p, "adjacent-date", first, last, p.getBirthDate().plusDays(1).toString());
            }
            Email email = emailParts(p.getEmail());
            if (email != null) add(blocks, p, "name-local", first, last, email.local);
        }
        SortedSet<Pair> pairs = new TreeSet<>();
        for (SortedSet<Integer> block : blocks.values()) {
            List<Integer> ids = new ArrayList<>(block);
            for (int i = 0; i < ids.size(); i++) for (int j = i + 1; j < ids.size(); j++)
                pairs.add(new Pair(ids.get(i), ids.get(j)));
        }
        return pairs;
    }
    private void add(Map<Key, SortedSet<Integer>> blocks, Person p, String rule, String... parts) {
        if (Arrays.stream(parts).anyMatch(String::isEmpty)) return;
        blocks.computeIfAbsent(new Key(rule, List.of(parts)), ignored -> new TreeSet<>()).add(p.getId());
    }
    public String fingerprint(Person left, Person right) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            for (Person p : List.of(left, right)) {
                for (String field : Arrays.asList(p.getUserName(), p.getFirstName(), p.getLastName(), p.getEmail(),
                        p.getBirthDate() == null ? null : p.getBirthDate().toString())) {
                    if (field == null) out.writeInt(-1);
                    else { byte[] value = field.getBytes(StandardCharsets.UTF_8); out.writeInt(value.length); out.write(value); }
                }
                out.writeLong(p.getVersion());
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch (IOException | NoSuchAlgorithmException e) { throw new IllegalStateException("Fingerprint unavailable"); }
    }
}
