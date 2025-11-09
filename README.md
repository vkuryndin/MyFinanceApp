# MyFinanceApp

![Build Status](https://github.com/vkuryndin/MyFinanceApp/actions/workflows/ci.yml/badge.svg)

![Coverage](./badges/jacoco.svg)
![Branches](./badges/branches.svg)

# Description

MyFinanceApp is a multi-user console application for managing personal finances. It supports incomes, expenses, budgets, statistics, JSON import/export, and user roles (User, Admin, Super Admin).  
The project includes a complete JUnit test suite and a GitHub CI/CD pipeline.

---

## Features

### Finance Management
- Add incomes and expenses
- View wallet and balance
- Show income and expense totals
- Category-based statistics
- Budgets with warnings (80%, 90%) and exceeded alerts
- Advanced statistics with date ranges and category filters

### Users and Roles
- Multi-user JSON storage
- Roles: USER, ADMIN, SUPER_ADMIN
- Administrator management (Super Admin)
- View statistics for all users
- Money transfers between users

### JSON Import and Export
- Wallet export to JSON
- Wallet import with duplicate detection
- Malformed JSON detection with readable error messages

### Console Interface
- Safe validated input (strings, numbers, dates)
- Centralized menu rendering

### Testing and Static Analysis
- JUnit 5 unit and integration tests
- Main application flow tested by MainTest.java
- SpotBugs and Checkstyle validation
- Jacoco coverage enabled

---

## Architecture Overview
    src/
    └── main/java/org/example/
        app/
          Main.java               # application entry point (menus + flow)
          MainTest.java           # integration tests for Main.java
        
        cli/
          ConsoleInput.java       # robust console input helpers (strings, numbers, dates)
          ConsoleMenus.java       # all menus (login, actions, admin)
          ConsoleInputTest.java
          ConsoleMenusTest.java
       
        util/
         ConsoleUtils.java       # handlers: add income/expense, budgets, stats, transfer, JSON ops
         ConsoleUtilsTest.java
       
        model/
         User.java               # id/login/name/surname, roles (USER/ADMIN/SUPER_ADMIN), password hash
         Wallet.java             # transactions, budgets, sums, category stats, alerts
         Transaction.java        # immutable transaction with id/date/type/title/amount
         UserTest.java
         WalletTest.java
       
        repo/
         UsersRepo.java          # in-memory users store; create/find/delete/transfer; role checks
         RepoExceptions.java     # domain errors (Invalid, NotFound, Forbidden)
         UsersRepoTest.java
       
        storage/
         StorageJson.java        # save/load entire UsersRepo to a JSON file
         WalletJson.java         # export/import a single user's wallet (transactions, budgets)
         StorageJsonTest.java
         WalletJsonTest.java

## Used Libraries

### Runtime
- Gson (JSON serialization)
- BCrypt (password hashing)

### Development and Testing
- JUnit 5
- Mockito (optional)
- Jacoco
- SpotBugs
- Checkstyle

---

## How to Build and Run

### Build the project
Run the following command:

    ./gradlew clean build

### Run the application

    java -jar build/libs/MyFinanceApp-1.0-SNAPSHOT.jar

---

## Running Tests and Coverage

### Run all tests

    ./gradlew test

### Generate coverage report

    ./gradlew jacocoTestReport

Reports can be found in:

    - build/reports/jacoco/
    - build/reports/tests/test/

---

## GitHub CI/CD Pipeline

GitHub Actions is fully configured to run on every push and pull request.

### The pipeline performs:
- Java 17 setup
- Gradle caching
- Full project build, including:
    - Compilation
    - Tests
    - Jacoco coverage
    - SpotBugs
    - Checkstyle

### Artifact Publishing
The pipeline uploads:
- JAR files
- JUnit test reports
- Jacoco HTML reports
- SpotBugs reports
- Checkstyle reports

### Coverage Badges
- Total and branch coverage badges are generated automatically
- Generated using jacoco-badge-generator
- Badges are committed back to the repository
- Badge generation disabled for pull requests

### Artifact Naming
- Branch names are sanitized (slashes replaced with dashes)

### Reliability
- Artifacts are stored for 7 days
- Reports are uploaded even if the build fails

---
