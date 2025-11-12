# Concurrent Ticket Booking Service
This repository contains the Java Spring Boot microservice responsible for managing event inventory and processing ticket booking transactions safely under high concurrency.

## Project Overview

### Description
A high-integrity Spring Boot application handling ticket inventory using a transactional database approach to prevent overselling.

### Objective
To implement RESTful endpoints for viewing event availability and booking tickets, ensuring transactional integrity and safety against race conditions during concurrent booking requests.

## Approach
The core challenge of this project was guaranteeing that the total number of tickets booked does not exceed the available inventory, even when many users attempt to book simultaneously.

## Architecture Decisions
The entire transaction is based on a _Pessimistic_ Locking strategy provided by Spring Data JPA and Hibernate.

1. **Concurrency Control (Pessimistic Locking):**
   - The bookTickets method in the TicketService is marked with @Transactional.

    - The EventRepository defines a custom method: _findByIdForUpdate()_ using _@Lock(LockModeType.PESSIMISTIC_WRITE)_.

    - When a booking request comes in, the transaction attempts to acquire a write lock on the specific database row for that event.

    - **Result:** Any subsequent concurrent transaction attempting to lock the same event row is forced to wait until the first transaction commits or rolls back, effectively serializing access to the critical inventory check-and-update step. This guarantees correctness and prevents overbooking.
      
2. **DTO (Data Transfer Object) Usage:**
    - The application uses a dedicated EventDTO (record class) to expose data to the frontend, ensuring the internal Event entity structure and any sensitive fields are not leaked outside the API boundary.
3. **Testing:**
    - A dedicated multi-threaded Integration Test _(TicketServiceConcurrencyTest.java)_ was created to simulate 20 concurrent booking threads targeting the same 100-ticket event, verifying that exactly 10 transactions succeed and the final ticket count is exactly zero.

### Key Components

   - Event : JPA Entity (Internal DB Model).
   - EventDTO :Data exposed to the Frontend (Public API Model).
   - EventRepository : Data Access Layer.
   - TicketService : Business Logic.
   - TicketController : REST Endpoints.
    
### Trade-offs and Assumptions
   - Concurrency Type : 
      Pessimistic locking : Provides maximum transactional safety (correctness) at the expense of throughput (scalability). Correctness was prioritized for this assessment.
   - Database : 
      H2 in-memory is sufficient : Meets the requirement. In production, this would be replaced by a persistent RDBMS like PostgreSQL.
   - Error Handling : 
      Relies on @ResponseStatus exceptions: Simple for basic error mapping (404, 400). A production service would use a global **_@ControllerAdvice_** for consistent error response bodies.

### Performance Considerations
The main performance bottleneck is the Pessimistic Lock on the event's database row during booking.
   - Under low-to-medium load, this approach is fast and safe.
   - Under extreme, concurrent high-load (e.g., a flash sale for a single event), many transactions would queue up at the database layer, potentially leading to slow response times and connection pool exhaustion.
   - Mitigation: The application is highly efficient for other operations (e.g., listing all events) as these are non-transactional and read-only.

### Challenges Faced
   - Concurrency Verification: Designing the JUnit test to reliably simulate the race condition using ExecutorService and CountDownLatch was the most complex part, ensuring threads start simultaneously to trigger the lock contention.
   - Transaction Scope: Ensuring the _@Transactional_ boundary properly encompasses the lock acquisition, inventory check, and update (read-check-write) was crucial for integrity.

### Project Setup

## Prerequisites
   - Java 17 (or newer) JDK
   - Apache Maven (v3.x or newer)
     
## Installation

**Clone the repository**:
   ```bash
   git clone https://github.com/PranavMS007/election-results-backend.git
   cd ConcurrentTicketBookingService
   ```
**Build the project**:
   ```bash
   mvn clean install
   ```
**Running the Application**
   ```bash
   mvn spring-boot:run
   ```

The application will start on port 8080.

REST API: http://localhost:8080/tickets (Base URL)

H2 Console: http://localhost:8080/h2-console (JDBC URL: jdbc:h2:mem:ticketdb)

### API Endpoints
   - GET : /tickets - eturns a list of all events from DB.
   - GET : /tickets/{id} - Returns details for a single event. 
   - POST : /tickets/{id}/book?count={N} -Books N tickets for the event. This is the protected, concurrent endpoint.

### Known Issues and Limitations
   - **No User ID:** The booking transaction is currently anonymous, lacking a user identifier.
   - **No Audit Trail:** The system only decrements the counter. It does not create a Booking record to log who booked which tickets, making it non-auditable.

### Future Improvements
1. **Introduce a Booking Entity:** Implement a transactional history table to log every successful and failed booking attempt, addressing the audit trail limitation.
2. **Add Authentication/Authorization:** Integrate Spring Security to secure endpoints and identify users.
3. **Implement Optimistic Locking:** Explore using a version column (@Version) as an alternative to pessimistic locking, potentially increasing throughput for events with lower contention risk.
4. **Global Error Handling:** Replace basic @ResponseStatus with a robust @ControllerAdvice for consistent JSON error structures.

### Scalability Considerations
The most crucial future improvement for production readiness is migrating to an Asynchronous Booking Pipeline (using a Message Queue like Kafka or RabbitMQ) to decouple the heavy transaction from the HTTP request. This would drastically improve the service's throughput under flash sale conditions, shifting from correctness-via-waiting _(pessimistic lock)_ to correctness-via-serialization (single-consumer queue).
