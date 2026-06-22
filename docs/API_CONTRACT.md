# Backend Analysis & API Contract

**Project:** Sentiment Analysis Platform  
**Stack:** Spring Boot 3.5, Java 17, MongoDB, Redis, Kafka, Spring AI (Ollama), STOMP WebSocket, JWT

---

## Backend analysis summary

### Architecture

| Layer | Packages |
|-------|----------|
| Auth | `public_controller`, `jwt`, `security` |
| Domain | `user`, `posts`, `comment`, `reaction` |
| Infrastructure | `redis`, `ai/kafka`, `realtime_websocket` |
| Cross-cutting | `global_handler_exception`, `config_cross`, `checker` |

### Spring Security

- **Stateless** JWT sessions, CSRF disabled, CORS enabled
- **Public:** `OPTIONS /**`, `/public/**`, `/ws/**`, `/api/auth/**`, `/api/health_checker`, `anyRequest()` fallback
- **Authenticated:** `/api/user/**`, `/api/posts/**`, `/api/comment/**`, `/api/react/**`
- **Admin:** `/Admin_userInfo/**` → `hasRole("ADMIN")`
- **Roles:** `USER` assigned on signup; `ADMIN` must be set manually in MongoDB

### JWT flow

1. Login → access token (`type=access`) + refresh token (`type=refresh`, stored in MongoDB)
2. Protected requests → `Authorization: Bearer <accessToken>`
3. Refresh → `{ refreshToken }` → new `{ accessToken }`
4. Logout → deletes refresh token from DB

### Redis

| Key | Purpose | TTL |
|-----|---------|-----|
| `feed:{email}:page:{n}:size:{s}` | Paginated feed cache | 30s |
| `comment:count:{postId}` | Comment counter | Persistent |
| `reaction:count:{postId}` | Reaction counter | Persistent |

### Kafka

- **Topic:** `comments-topic`
- **Producer:** `CommentService.newComment` → key=commentId, value=`commentId::text`
- **Consumer:** `SentimentConsumer` → gibberish check → `SentimentAiService.analyze()` → updates MongoDB

### Spring AI

- Ollama via `spring-ai-starter-model-ollama`
- `SentimentAiService` returns `{ sentiment: POSITIVE|NEGATIVE|NEUTRAL, confidence: Double }`
- Failure → HTTP 503

### WebSocket

- Endpoint: `/ws` (SockJS)
- Publishes to: `/topic/posts/{postIdHex}`
- Events: `COMMENT_CREATED`, `COMMENT_DELETED`, `REACTION_CREATED`, `REACTION_DELETED`, `REACTION_UPDATED`

---

## Feature mapping table

| Controller | Endpoint | Method | Request | Response | Auth | Roles | Frontend |
|------------|----------|--------|---------|----------|------|-------|----------|
| AuthController | `/api/auth/sign_up` | POST | `{ userEmail, password }` | `Users` (201) | Public | — | RegisterPage |
| AuthController | `/api/auth/login` | POST | `{ userEmail, password }` | `{ accessToken, refreshToken }` or error string | Public | — | LoginPage |
| AuthController | `/api/auth/refresh` | POST | `{ refreshToken }` | `{ accessToken }` | Public | — | axios interceptor |
| AuthController | `/api/auth/logout` | POST | `{ refreshToken }` | `{ message }` | Public | — | Navbar logout |
| AuthController | `/api/auth/logout-all` | POST | Query `userEmail` | `{ message }` | Public | — | ProfilePage |
| FrontControllerOfPosts | `/api/posts` | GET | `page`, `size`, `sort` | `ApiResponse<PaginatedResponse<PostResponseDto>>` | JWT | USER+ | FeedPage |
| FrontControllerOfPosts | `/api/posts` | POST | `PostDto` | `Posts` (201) | JWT | USER+ | CreatePostPage |
| FrontControllerOfPosts | `/api/posts/{id}` | PUT | `PostDto` | `Posts` | JWT | Owner | PostDetailPage |
| FrontControllerOfPosts | `/api/posts/{id}` | DELETE | — | 204 | JWT | Owner | PostDetailPage |
| FrontControllerOfPosts | `/api/posts/{postId}` | GET | — | `ApiResponse<PostDetailDto>` | JWT | Visible posts | PostDetailPage |
| CommentController | `/api/comment` | GET | `page`, `size` | `ApiResponse<PaginatedResponse<CommentResponseDto>>` | JWT | USER+ | MyCommentsPage, sentiment merge |
| CommentController | `/api/comment` | POST | `{ postId, text }` | `CommentResponseDto` (201) | JWT | USER+ | PostDetailPage |
| CommentController | `/api/comment/{id}` | PUT | `CommentRequest` | `Comment` | JWT | Owner | *(blocked: no comment id in list DTOs)* |
| CommentController | `/api/comment/{id}` | DELETE | — | 204 | JWT | Owner/Post owner | *(blocked: no comment id in list DTOs)* |
| CommentController | `/api/comment/{postId}/comments/count` | GET | — | `Long` | JWT | USER+ | Available via services (counts in feed/detail) |
| ReactionController | `/api/react` | GET | `page`, `size` | `ApiResponse<PaginatedResponse<ReactionResponseDto>>` | JWT | USER+ | MyReactionsPage |
| ReactionController | `/api/react` | POST | `{ postId, reactionType }` | void (200) | JWT | USER+ | PostDetailPage |
| ReactionController | `/api/react/{postId}/count` | GET | — | `Long` | JWT | USER+ | Available via services |
| UserController | `/api/user/{id}` | PUT | `UserDto` | `Users` | JWT | Self only | ProfilePage |
| UserController | `/api/user/{id}` | DELETE | — | 204 | JWT | Self only | ProfilePage |
| AdminInfo | `/Admin_userInfo/getAllUser` | GET | `page`, `size` | `ApiResponse<PaginatedResponse<UserResponse>>` | JWT | ADMIN | AdminPage |
| HealthChecker | `/api/health_checker` | GET | — | `"Ok fine"` | Public | — | HealthPage |
| WebSocket | `/ws` → `/topic/posts/{postId}` | SUB | — | `PostRealtimeUpdateDto` | Public connect | — | PostDetailPage |

### DTO reference

**PostResponseDto:** `postId`, `title`, `text`, `userEmail`, `createAt`, `commentCount`, `reactionCount`

**PostDetailDto:** `post`, `comments[]`, `reactions[]`

**CommentResponseDto:** `postId`, `userEmail`, `text`, `sentiment`, `confidence`, `createAt`

**CommentResponseDetailDto:** `text`, `userEmail`, `createAt` *(no sentiment)*

**ReactionResponseDto:** `postId`, `userEmail`, `reactionType`, `createdAt`

**ReactionResponseDetailDto:** `reactionType`, `userEmail`, `createAt`

**UserResponse:** `userEmail`, `dateTime`

**Enums:** `TypeOfAccess`: PUBLIC, PRIVATE | `ReactionType`: HAHA, LIKE, SAD, HAPPY, LOVE | `SentimentType`: POSITIVE, NEGATIVE, NEUTRAL

---

## Suggested backend improvements

1. Add `GET /api/user/me` returning id, email, roles (no password)
2. Add `commentId` to comment response DTOs
3. Include `sentiment` and `confidence` in `CommentResponseDetailDto`
4. Assign or expose ADMIN role management
5. Protect `logout-all` with authentication
6. Add `@JsonIgnore` on `Users.password`
7. Re-encode password in `UserService.newUserUpdate`
8. Invalidate Redis feed cache on post/comment/reaction mutations
