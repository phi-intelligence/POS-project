
# Overall Flow of a Request

Understand the **typical request flow**:

```text
Client Request
      ↓
Controller
      ↓
Service
      ↓
Repository
      ↓
Database
```

Example:

```text
POST /auth/login
      ↓
AuthController
      ↓
AuthService
      ↓
UserRepository
      ↓
PostgreSQL
```

---

# 1️⃣ `config`

```
config
```

This folder contains **application configuration classes**.

Examples:

* Security configuration
* CORS configuration
* Kafka configuration
* Redis configuration
* Bean configuration

Example files:

```
SecurityConfig.java
KafkaConfig.java
RedisConfig.java
CorsConfig.java
```

Example:

```java
@Configuration
public class SecurityConfig {
}
```

Purpose:

```
Configure how Spring Boot behaves
```

---

# 2️⃣ `controller`

```
controller
```

Controllers define **API endpoints**.

They receive **HTTP requests from clients**.

Example:

```
AuthController.java
UserController.java
HealthController.java
```

Example API:

```java
@RestController
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/login")
    public LoginResponse login() {
        return new LoginResponse();
    }
}
```

Example request:

```
POST /auth/login
```

Controllers should **not contain business logic**.

---

# 3️⃣ `dto`

```
dto
```

DTO = **Data Transfer Object**

Used for **API request and response models**.

Example:

```
LoginRequest
RegisterRequest
UserResponse
```

Example:

```java
public class LoginRequest {
    private String username;
    private String password;
}
```

DTO purpose:

```
Client ↔ API communication
```

Instead of exposing database models directly.

---

# 4️⃣ `events`

```
events
```

This folder is used for **event-driven communication**.

Since your system uses **Kafka**, this folder will contain:

```
Event classes
Event producers
Event consumers
```

Example:

```
UserCreatedEvent.java
PaymentConfirmedEvent.java
```

Example event:

```java
public class UserCreatedEvent {
    private UUID userId;
    private String email;
}
```

When user registers:

```
identity-service
      ↓
Kafka event published
      ↓
notification-service receives event
```

---

# 5️⃣ `model`

```
model
```

This folder contains **database entities**.

Each class represents a **database table**.

Example:

```
User.java
Role.java
Permission.java
```

Example:

```java
@Entity
public class User {

    @Id
    private UUID id;

    private String username;
}
```

Table created:

```
users
```

---

# 6️⃣ `repository`

```
repository
```

Repositories handle **database operations**.

Spring Data JPA automatically generates queries.

Example:

```
UserRepository.java
RoleRepository.java
```

Example:

```java
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

}
```

Responsibilities:

```
Database queries
CRUD operations
```

---

# 7️⃣ `security`

```
security
```

This folder contains **authentication and authorization logic**.

Example files:

```
JwtService.java
JwtFilter.java
UserDetailsServiceImpl.java
PasswordEncoderConfig.java
```

Responsibilities:

```
JWT token generation
Authentication
Authorization
Password encryption
```

Example:

```java
public class JwtService {

    public String generateToken(User user) {
        return "jwt-token";
    }

}
```

---

# 8️⃣ `service`

```
service
```

This is where **business logic lives**.

Controllers call services.

Example:

```
AuthService.java
UserService.java
RoleService.java
```

Example:

```java
@Service
public class AuthService {

    public LoginResponse login(LoginRequest request) {
        return new LoginResponse();
    }

}
```

Responsibilities:

```
Business logic
Validation
Complex workflows
```

---

# 9️⃣ `resources`

```
resources
```

Contains **application configuration and static resources**.

Example:

```
application.properties
application.yml
db/migration
```

Also contains:

```
static
templates
```

Though in microservices these are rarely used.

---

# 🔟 `test`

```
test
```

Contains **unit tests and integration tests**.

Example:

```
AuthServiceTest.java
UserControllerTest.java
```

Testing ensures your service works correctly.

---

# Example Identity Service Structure

Your service will eventually look like this:

```
identity-service

config
controller
dto
events
model
repository
security
service
```

Example files:

```
controller
 ├ AuthController
 └ UserController

service
 ├ AuthService
 └ UserService

repository
 ├ UserRepository
 └ RoleRepository

model
 ├ User
 ├ Role
 └ Permission
```

---

# How Everything Works Together

Example login request:

```
POST /auth/login
```

Flow:

```
AuthController
      ↓
AuthService
      ↓
UserRepository
      ↓
PostgreSQL
```

Then:

```
JWT generated
```

---

# Summary

| Folder     | Responsibility                 |
| ---------- | ------------------------------ |
| config     | Application configuration      |
| controller | REST API endpoints             |
| dto        | API request/response models    |
| events     | Kafka event classes            |
| model      | Database entities              |
| repository | Database queries               |
| security   | Authentication / authorization |
| service    | Business logic                 |
| resources  | App configuration              |
| test       | Unit tests                     |

---