# Customer Support & WebSocket Service

The **Customer Support Service** is the real-time communication hub of the Energy Management System. It handles live chat functionality using a hybrid approach (Rule-based + AI + Human Admin) and delivers push notifications to users via WebSockets.

### ⚙️ Technology Stack

* **Language**: Java 21
* **Framework**: Spring Boot 3.x, Spring WebFlux
* **Messaging**: RabbitMQ (AMQP), WebSocket (STOMP/SockJS)
* **AI Integration**: Google Gemini API
* **Security**: Spring Security (JWT Token extraction via Headers)

### 🚀 Features & Architecture

#### 1. Real-Time Chat System
The service provides a chat interface for users. Incoming user messages are processed in the following priority order:
1.  **Admin Broadcast**: All user messages are forwarded to the `/topic/admin-chat` so Administrators can monitor conversations.
2.  **Rule-Based Engine**: The system checks for keyword patterns (e.g., "hello", "login", "reset password") to provide instant, predefined answers.
3.  **Generative AI (Gemini)**: If no rule matches, the message is sent to the Google Gemini API to generate a context-aware, helpful response.
4.  **Admin Intervention**: Administrators can intercept and manually reply to specific users via the `/chat.adminResponse` endpoint.

#### 2. Overconsumption Alerts (Push Notifications)
* **Source**: Listens to the `overconsumption.alert.queue` from RabbitMQ (sent by the Monitoring Service).
* **Delivery**: When an alert is received, it is pushed instantly to the specific user's frontend via the WebSocket destination `/user/queue/alerts`.

#### 3. WebSocket Security
* Implements a `WebSocketAuthInterceptor` that intercepts the STOMP `CONNECT` frame.
* Extracts the `X-User-Id` header (injected by the API Gateway/Traefik) to establish a secure `Principal` for the WebSocket session, ensuring users only receive messages intended for them.

### 📡 WebSocket Endpoints

| Endpoint | Type | Description |
| :--- | :--- | :--- |
| `/ws` | **Connect** | Main SockJS endpoint for client connection. |
| `/app/chat.sendMessage` | **Send** | Endpoint for users to send chat messages. |
| `/app/chat.adminResponse` | **Send** | Endpoint for Admins to reply to specific users. |
| `/user/queue/messages` | **Subscribe** | Users subscribe here to receive chat replies (System/AI/Admin). |
| `/user/queue/alerts` | **Subscribe** | Users subscribe here to receive energy overconsumption alerts. |
| `/topic/admin-chat` | **Subscribe** | Admins subscribe here to see all user messages. |

### 🛠️ Configuration

The service requires the following environment variables (defined in `.env`):

```yaml
spring:
  rabbitmq:
    host: synchronization-broker
    port: 5672

gemini:
  api:
    key: ${GEMINI_API_KEY} # Required for AI responses
```