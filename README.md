The Registry Office application is a simple civil registry system that allows managing individuals ("Persons") in a centralized repository.

The system supports:
- Registration of new individuals
- Retrieval of individual or multiple records
- Update of existing records
- Deletion of records
- Search across multiple attributes

The data is stored in-memory and is not persisted across application restarts.

# Get started

To run the application locally, the following tools shall be installed in the local machine:
  * [Java Runtime Environment](https://www.oracle.com/java/technologies/downloads/) v.21 (or later)
  * [Apache Maven](https://maven.apache.org/) v.3.8 (or later).

## 1. Clone repository
```bash
git clone https://gitlab.arhs-developments.com/ARHS/Training/code-assistant-use-cases/registry-office.git 

cd registry-office
```

## 2. Build the application
```bash
mvn clean install
```
Also, all tests will be executed locally.

## 3. Run the application
```bash
java -jar target/registry-office-?.?.?.jar
```
The application will start on port `8080`: send a sample request to `http://localhost:8080` to see if you're up and running.

---
---

# REST APIs
The sample application is almost as easy as it gets. It manages `Person` objects in an in-memory database using _Spring Data_.
You can execute HTTP requests against the following endpoints:

  * `GET /person`: Retrieves all persons currently present in the database. You can call it via cURL with:
    * curl --request GET 'http://localhost:8080/person'  
  * `GET /person/{id}`: Looks up the person with the provided _id_. If the person is found, it's returned. Call it with:
    * curl --request GET 'http://localhost:8080/person/1'
  * `GET /person/username/{userName}`: Looks up the person with the provided _userName_. If the person is found, it's returned. Call it with:
    * curl --request GET 'http://localhost:8080/person/username/panpe'
  * `GET /person/email/{email}`: Looks up the person with the provided _email_. If the person is found, it's returned. Call it with:
    * curl --request GET 'http://localhost:8080/person/email/peter.pan@gmail.com'
  * `GET /person/search/{inputText}`: Looks up persons with the provided _inputText_ in their username, firstname, lastname or email. If some persons are found, they're returned. Call it with:
    * curl --request GET 'http://localhost:8080/person/search/r.pan@gm'
  * `POST /person`: It creates a new person. Call it with:
    * curl --request POST 'http://localhost:8080/person' --header 'Content-Type: application/json' --data-raw '{"userName":"panpe", "firstName":"Peter", "lastName":"Pan", "email":"peter.pan@gmail.com", "birthDate":"2015-01-31"}'
  * `PUT /person`: It updates the person with the provided lastname. Call it with:
    * curl --request PUT 'http://localhost:8080/person' --header 'Content-Type: application/json' --data-raw '{"id": 1, "userName":"panpe", "firstName":"Peter", "lastName":"Pan", "email":"peter.pan@gmail.com", "birthDate":"2015-01-31"}'
  * `DELETE /person/{id}`: It deletes the existing person with the provided _id_. Call it with:
    * curl --request DELETE 'http://localhost:8080/person/1'
  * `DELETE /person/username/{userName}`: It deletes the existing person with the provided _userName_. Call it with:
    * curl --request DELETE 'http://localhost:8080/person/username/panpe'
  * `DELETE /person`: It deletes all persons currently existing in the database. Call it with:
    * curl --request DELETE 'http://localhost:8080/person'

---

# Business Functionalities

## 1. Actors

### End User
A user interacting with the system via API (or UI) to manage person records.

### System
The backend service responsible for processing requests and managing the registry.

---

## 2. Assumptions and Scope

- Each person is uniquely identified by an `id`
- `userName` and `email` are also treated as unique identifiers in lookup operations
- The system does not persist data (in-memory only)
- No authentication or authorization is implemented
- Validation rules are minimal and inferred from usage

---

## 3. Functionalities

---

## F-01 Create Person

### Goal
Allow a user to register a new person in the civil registry.

### Actors
- End User

### Preconditions
- The person does not already exist (based on unique fields such as username/email)

### Trigger
- User submits a creation request with person data

### Main flow
1. User provides person details (username, first name, last name, email, birth date)
2. System validates input
3. System creates a new person record
4. System assigns a unique identifier
5. System stores the person in the registry
6. System confirms successful creation

### Alternative flows / Exceptions
- If mandatory fields are missing → request is rejected
- If a duplicate username or email exists → creation may be rejected (assumed constraint)

### Business rules
- A person must have:
  - userName
  - firstName
  - lastName
  - email
  - birthDate
- Each person is uniquely identifiable

### Acceptance criteria

#### Successful creation
- Given valid person data
- When the user submits the creation request
- Then the system creates a new person and assigns an ID

#### Missing data
- Given incomplete input data
- When the user submits the request
- Then the system rejects the request

#### Duplicate handling
- Given a person with the same username or email already exists
- When the user submits the request
- Then the system prevents duplication (assumed)

### Expected result
A new person is stored and retrievable via the system.

---

## F-02 Retrieve All Persons

### Goal
Allow the user to view all registered persons.

### Actors
- End User

### Preconditions
- None

### Trigger
- User requests the list of all persons

### Main flow
1. User requests all persons
2. System retrieves all records
3. System returns the list

### Alternative flows / Exceptions
- If no persons exist → return empty list

### Business rules
- The system returns all available records without filtering

### Acceptance criteria

#### Data exists
- Given persons exist in the registry
- When the user requests all persons
- Then the system returns the full list

#### No data
- Given no persons exist
- When the user requests all persons
- Then the system returns an empty list

### Expected result
User receives a list of all persons.

---

## F-03 Retrieve Person by ID

### Goal
Allow the user to retrieve a specific person using their unique identifier.

### Actors
- End User

### Preconditions
- Person exists with given ID

### Trigger
- User requests a person by ID

### Main flow
1. User provides an ID
2. System searches for the person
3. System returns the matching record

### Alternative flows / Exceptions
- If no match → return not found

### Business rules
- ID uniquely identifies a person

### Acceptance criteria

#### Found
- Given a valid ID
- When the user searches
- Then the system returns the corresponding person

#### Not found
- Given an invalid or unknown ID
- When the user searches
- Then the system returns no result / error

### Expected result
The requested person is returned if present.

---

## F-04 Retrieve Person by Username or Email

### Goal
Allow lookup of a person using alternative unique identifiers.

### Actors
- End User

### Preconditions
- Person exists with given username or email

### Trigger
- User searches by username or email

### Main flow
1. User provides username or email
2. System searches registry
3. System returns matching person

### Alternative flows / Exceptions
- If no match → return not found

### Business rules
- Username and email are treated as unique identifiers

### Acceptance criteria

#### Found
- Given a valid username/email
- When the user searches
- Then the system returns the matching person

#### Not found
- Given no matching record
- When the user searches
- Then the system returns no result

### Expected result
Person is retrieved using alternative identifiers.

---

## F-05 Search Persons (Free Text)

### Goal
Allow users to search persons using partial or full text across multiple attributes.

### Actors
- End User

### Preconditions
- At least one person exists

### Trigger
- User provides search input

### Main flow
1. User enters search text
2. System searches across:
   - username
   - first name
   - last name
   - email
3. System returns matching persons

### Alternative flows / Exceptions
- No matches → empty list

### Business rules
- Search is partial and case-insensitive (inferred)
- Multiple results may be returned

### Acceptance criteria

#### Matches found
- Given persons exist matching the input
- When the user searches
- Then the system returns all matching persons

#### No matches
- Given no matching persons
- When the user searches
- Then the system returns an empty list

### Expected result
User receives a filtered list of persons.

---

## F-06 Update Person

### Goal
Allow modification of an existing person's data.

### Actors
- End User

### Preconditions
- Person exists

### Trigger
- User submits updated data including ID

### Main flow
1. User provides updated person data
2. System verifies the person exists
3. System updates the record
4. System confirms update

### Alternative flows / Exceptions
- If person does not exist → update fails

### Business rules
- ID is required for update
- All fields may be overwritten

### Acceptance criteria

#### Successful update
- Given a valid existing person
- When the user submits updated data
- Then the system updates the record

#### Person not found
- Given a non-existing ID
- When the user submits update
- Then the system rejects the request

### Expected result
The person’s data is updated in the registry.

---

## F-07 Delete Person (by ID or Username)

### Goal
Allow removal of a specific person.

### Actors
- End User

### Preconditions
- Person exists

### Trigger
- User requests deletion

### Main flow
1. User provides ID or username
2. System finds the person
3. System deletes the record
4. System confirms deletion

### Alternative flows / Exceptions
- If not found → no action or error

### Business rules
- Deletion is irreversible

### Acceptance criteria

#### Successful deletion
- Given a valid person
- When the user deletes
- Then the person is removed

#### Not found
- Given no matching person
- When the user deletes
- Then the system returns an error or no-op

### Expected result
The person is removed from the registry.

---

## F-08 Delete All Persons

### Goal
Allow clearing the entire registry.

### Actors
- End User

### Preconditions
- None

### Trigger
- User requests full deletion

### Main flow
1. User triggers delete-all operation
2. System removes all records
3. System confirms completion

### Alternative flows / Exceptions
- If already empty → no effect

### Business rules
- Operation affects all records
- No recovery mechanism

### Acceptance criteria

#### Data present
- Given persons exist
- When user deletes all
- Then all records are removed

#### No data
- Given no persons exist
- When user deletes all
- Then system remains empty

### Expected result
Registry is fully cleared.

---

## 5. Cross-functional Business Rules

- Each person has a unique identity (ID)
- Username and email are used for lookup and should be unique
- System is stateless across restarts (no persistence)
- All operations are immediate and synchronous

## 6. Project Tree (structure)

```bash
registry-office-main/
├── pom.xml                                                                  # Maven file defining dependencies, Java version, and build configuration.
├── README.md                                                                # Main project documentation (usage, run, endpoints).
├── .gitignore                                                               # Lists files and folders ignored by Git.
├── generated/                                                             
│   └── src/
│       └── main/java/com/arhs/tools/ai/
│           ├── config/OpenApiConfig.java                                    # Generated OpenAPI configuration.
│           └── controller/CivilRegistryController.java                      # Generated REST controller.
├── src/
│   ├── main/
│   │   ├── java/com/arhs/tools/ai/
│   │   │   ├── RegistryOffice.java                                          # Spring Boot entry point.
│   │   │   ├── config/OpenApiConfig.java                                    # API documentation configuration.
│   │   │   ├── controller/
│   │   │   │   ├── CivilRegistryController.java                             # REST endpoints for managing persons.
│   │   │   │   └── HomeController.java                                      # Redirects to home page.
│   │   │   ├── domain/
│   │   │   │   ├── Person.java                                              # Person model (fields and relationships).
│   │   │   │   └── util/PersonUtil.java                                     # Validation and age utility methods.
│   │   │   ├── exception/DuplicatePropertyException.java                    # Exception for duplicate unique values.
│   │   │   ├── repository/CivilRegistryRepository.java                      # Data access (database queries).
│   │   │   └── service/
│   │   │       ├── CivilRegistryService.java                                # Business operations interface.
│   │   │       └── CivilRegistryServiceImpl.java                            # Business logic implementation.
│   │   └── resources/
│   │       ├── application.properties                                       # Application configuration.
│   │       └── static/
│   │           ├── index.html                                               # Main web page UI.
│   │           ├── app.js                                                   # Frontend logic and API calls.
│   │           └── style.css                                                # UI styling.
│   └── test/
│       ├── java/com/arhs/tools/ai/
│       │   ├── controller/CivilRegistryControllerTest.java                  # Controller tests.
│       │   ├── domain/util/PersonUtilTest.java                              # Validation and age tests.
│       │   └── service/CivilRegistryServiceTest.java                        # Service (business logic) tests.
│       └── resources/logback.xml                                            # Test logging configuration.
```
