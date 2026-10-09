package com.arhs.tools.ai.controller;

import com.arhs.tools.ai.domain.Person;
import com.arhs.tools.ai.exception.DuplicatePropertyException;
import com.arhs.tools.ai.service.CivilRegistryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(path = "/person", produces = MediaType.APPLICATION_JSON_VALUE)
public class CivilRegistryController {

    private final CivilRegistryService service;

    public CivilRegistryController(final CivilRegistryService service) {
        this.service = service;
    }

    // 1) GET /person: Retrieves all persons
    @GetMapping
    public ResponseEntity<List<Person>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    // 2) GET /person/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable int id) {
        Optional<Person> person = service.find(id);
        if (person.isEmpty()) {
            return notFound("Person with id %d not found".formatted(id));
        }
        return ResponseEntity.ok(person.get());
    }

    // 3) GET /person/username/{userName}
    @GetMapping("/username/{userName}")
    public ResponseEntity<?> getByUserName(@PathVariable String userName) {
        Optional<Person> person = service.findByUserName(userName);
        if (person.isEmpty()) {
            return notFound("Person with username '%s' not found".formatted(userName));
        }
        return ResponseEntity.ok(person.get());
    }

    // 4) GET /person/email/{email}
    @GetMapping("/email/{email:.+}")
    public ResponseEntity<?> getByEmail(@PathVariable String email) {
        Optional<Person> person = service.findByEmail(email);
        if (person.isEmpty()) {
            return notFound("Person with email '%s' not found".formatted(email));
        }
        return ResponseEntity.ok(person.get());
    }

    // 5) GET /person/search/{inputText}
    @GetMapping("/search/{inputText}")
    public ResponseEntity<?> search(@PathVariable String inputText) {
        List<Person> persons = service.search(inputText);
        if (persons.isEmpty()) {
            return notFound("No persons found for search '%s'".formatted(inputText));
        }
        return ResponseEntity.ok(persons);
    }

    // 6) POST /person: create new person
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(@RequestBody Person person) {
        try {
            Person created = service.create(person);
            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(created.getId())
                    .toUri();
            return ResponseEntity.created(location).body(created);
        } catch (DuplicatePropertyException e) {
            return conflict(e.getMessage());
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    // 7) PUT /person: update person (expects an id in payload)
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(@RequestBody Person person) {
        if (person == null || person.getId() <= 0) {
            return badRequest("A valid 'id' must be provided to update a person");
        }
        try {
            Optional<Person> updated = service.update(person);
            if (updated.isEmpty()) {
                return notFound("Person with id %d not found".formatted(person.getId()));
            }
            return ResponseEntity.ok(updated.get());
        } catch (DuplicatePropertyException e) {
            return conflict(e.getMessage());
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    // 8) DELETE /person/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteById(@PathVariable int id) {
        Optional<Person> deleted = service.delete(id);
        if (deleted.isEmpty()) {
            return notFound("Person with id %d not found".formatted(id));
        }
        return ResponseEntity.ok(deleted.get());
    }

    // 9) DELETE /person/username/{userName}
    @DeleteMapping("/username/{userName}")
    public ResponseEntity<?> deleteByUserName(@PathVariable String userName) {
        Optional<Person> deleted = service.deleteByUserName(userName);
        if (deleted.isEmpty()) {
            return notFound("Person with username '%s' not found".formatted(userName));
        }
        return ResponseEntity.ok(deleted.get());
    }

    // 10) DELETE /person/email/{email}
    @DeleteMapping("/email/{email:.+}")
    public ResponseEntity<?> deleteByEmail(@PathVariable String email) {
        Optional<Person> deleted = service.deleteByEmail(email);
        if (deleted.isEmpty()) {
            return notFound("Person with email '%s' not found".formatted(email));
        }
        return ResponseEntity.ok(deleted.get());
    }

    // 11) DELETE /person: delete all
    @DeleteMapping
    public ResponseEntity<List<Person>> deleteAll() {
        List<Person> deleted = service.deleteAll();
        return ResponseEntity.ok(deleted);
    }

    // --- helpers
    private ResponseEntity<Map<String, String>> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", message));
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
    }

    private ResponseEntity<Map<String, String>> conflict(String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", message));
    }

    // Ensure non-2xx responses include a clear message for typical parse/validation errors
    @ExceptionHandler({ HttpMessageNotReadableException.class })
    public ResponseEntity<Map<String, String>> handleNotReadable(HttpMessageNotReadableException ex) {
        return badRequest("Request body is invalid: " + rootMessage(ex));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("Validation failed");
        return badRequest(message);
    }

    private String rootMessage(Throwable t) {
        Throwable curr = t;
        while (curr.getCause() != null) {
            curr = curr.getCause();
        }
        return curr.getMessage() == null ? t.getMessage() : curr.getMessage();
    }
}