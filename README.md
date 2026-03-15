# 🦸🏻 KIERO
<br/>

> 💻 개발 기간 | 2025.12.27 ~ 진행중

<p align="center">
  <img width="800" alt="kiero-main" 
       src="https://github.com/user-attachments/assets/223d0845-fb1e-4cdc-bd15-8ab5f26c8126" />
</p>


<p align="center">
  ✤ <strong>APP STORE</strong> | 준비중 <br/>
  ✤ <strong>GOOGLE PLAYSTORE</strong> | 준비중
</p>

---
<br/>

## 👾 서비스 소개
<img width="822" height="462" alt="main" src="https://github.com/user-attachments/assets/b628829f-4a2a-4c70-ad12-c23688391cbc" />

> 잔소리 대신 모험을, 감시 대신 믿음을 선물하는 부모와 아이의 즐거운 연결 솔루션
<br/>
 
<img width="976" height="546" alt="parent_schedule" src="https://github.com/user-attachments/assets/43a45b4a-045c-4688-b84d-5fd3882c4d23" />
<img width="943" height="531" alt="parent_schedule_ai" src="https://github.com/user-attachments/assets/3817f934-0ac3-4cdb-a294-827c62bda94f" />
<img width="853" height="480" alt="parent_feed" src="https://github.com/user-attachments/assets/8fef0a42-bd92-4ae8-bf23-72ff6bb7f39f" />

> 부모의 복잡한 머릿속을 정리하는 스마트한 관리
<br/>

<img width="916" height="515" alt="child_main" src="https://github.com/user-attachments/assets/c9341a61-ef6c-4d10-ad9d-39e32d664330" />
<img width="791" height="446" alt="child_sub" src="https://github.com/user-attachments/assets/d89f5149-2ca9-4cd7-a8c7-8555a2b88237" />

> 오늘의 일정이 재미있는 나만의 모험으로, 내 노력은 현실의 보상으로
<br/>

## 🐶 키어로 서버

<br/>

<table align="center">
  <tr>
    <td align="center" width="300">
     <img width="540" height="860" alt="kiero_sae2say" src="https://github.com/user-attachments/assets/a359f700-5af9-4645-a659-516773744cc7" /><br/>
      <b>백세희</b><br/>
      <code>Server Developer</code><br/>
      <a href="https://github.com/sae2say">GitHub | @sae2say</a>
    </td>
    <td align="center" width="300">
      <img width="540" height="860" alt="kiero_dietken01" src="https://github.com/user-attachments/assets/d071f926-bcb2-4e71-9359-050c3a6afa49" /><br/>
      <b>정원준</b><br/>
      <code>Server Developer</code><br/>
      <a href="https://github.com/dietken1">GitHub | @dietken1</a>
    </td>
  </tr>
</table>
<br/>

## ⚙️ Tech
### ✤ Stack
| 👾 Category | ⚒️ Tech | 📝 Description |
|:----------:|:------:|:-------------:|
| **`Backend / Application`** | Spring Boot, Spring AI | 핵심 비즈니스 로직 및 API 서버 구현 |
| **`Web / Reverse Proxy`** | Nginx | 요청 라우팅 및 리버스 프록시 |
| **`DB / Storage / Cache`** | AWS RDS (MySQL), AWS S3, Redis | 영속 데이터 저장, 파일 저장 및 캐싱 처리 |
| **`Cache/Inmemory Store`** | Redis | 캐싱, 토큰 관리 및 빠른 데이터 조회 |
| **`Monitoring`** | Prometheus, Grafana | 서버 메트릭 수집 및 시각화 모니터링 |
| **`Error Tracking`** | Sentry | 에러 추적 및 Slack/Discord 알림 |
| **`Uptime Monitoring`** | BetterStack, UptimeRobot | 헬스체크 및 장애 알림 |
| **`Infrastructure`** | AWS EC2, Docker, GitHub Actions | 서버 인프라 구성 및 CI/CD 자동화 |
| **`Collaboration Tools`** | GitHub, Notion, Swagger, Figma, NocoDB | 협업, 문서화 및 API 명세 관리 |

<br/>

### ✤ Infra Flow 

<p align="center">
  <img width="820" 
       alt="tech-stack" 
       src="https://github.com/user-attachments/assets/dbea273a-c3bd-4bbd-b890-bd8b501bddda" />
</p>

<br/>

### 📊 ERD
<br/>
<p align="center">
<img width="1122" height="673" alt="image" src="https://github.com/user-attachments/assets/76b47c3a-82e8-474a-aa0b-0817ee57198b" />
</p>

## 📏 Convention
### Git
**✤ `Git Workflow`**
- **`Issue`** → **`Branch`** → **`PR`** → **`Review`** → **`Merge`** 의 흐름을 준수한다.
- `develop` 브랜치에서 직접 작업하지 않는다. (README, 템플릿 제외)
- PR은 **squash and merge** 사용한다.
<br/>

**✤ `Branch & Commit`**
- **Branch**: `<prefix>/<issue-number>` e.g. `feat/12`, `fix/31`
- **Commit Message**: `[#이슈번호] <prefix>(Class): 설명`  e.g. `[#1] feat(User): 로그인 API 구현`
  <br/>

**✤ `Pull Request`**
- PR 제목: `[prefix] #이슈번호 - 설명`
- 코드 리뷰 필수 (존댓말, 최소 이모지 리액션)

---

### Code
**✤ `Normal Naming`**
- **Class**: PascalCase  
- **Method / Variable**: camelCase  
- **DB Table**: snake_case  
- **Enum / Constant**: UPPERCASE_SNAKE_CASE  

**✤ `Method Naming`**
- 조회: `find...()`
- 생성: `create...()` / `generate...()`
- 수정: `update...()` / `modify...()`
- 삭제: `delete...()` / `remove...()`

**✤ `Url Rule`**
- RESTful API 준수
- 소문자, 복수형 사용
- `_` 대신 `-` 사용
- 행위(get, post 등)는 URL에 포함 X

**✤ `Dto Naming`**
- **Request**: `ResourceActionRequest`
- **Response**: `ResourceActionResponse`
  
<br/>

## 🌳 Package Structure
```src/main/java/com/kiero
├── child
│   ├── domain
│   ├── exception
│   ├── presentation
│   │   └── dto
│   ├── repository
│   └── service
├── coupon
│   ├── domain
│   ├── exception
│   ├── presentation
│   │   └── dto
│   ├── repository
│   └── service
├── feed
│   ├── domain
│   │   └── enums
│   ├── exception
│   ├── infrastructure
│   │   ├── converter
│   │   └── event
│   │       └── dto
│   ├── presentation
│   │   └── dto
│   ├── repository
│   └── service
├── holiday
│   ├── domain
│   ├── dto
│   ├── repository
│   ├── scheduler
│   └── service
├── invitation
│   ├── domain
│   ├── enums
│   ├── exception
│   ├── repository
│   ├── service
│   └── util
├── mission
│   ├── domain
│   ├── exception
│   ├── presentation
│   │   └── dto
│   ├── repository
│   └── service
├── parent
│   ├── domain
│   ├── exception
│   ├── presentation
│   │   └── dto
│   ├── repository
│   └── service
│       └── socialService
├── schedule
|   ├── domain
|   │   └── enums
|   ├── exception
|   ├── presentation
|   │   └── dto
|   ├── repository
|   ├── scheduler
|   └── service
|       └── resolver
├── global
    ├── aop
    ├── api
    ├── auth
    │   ├── annotation
    │   ├── client
    │   │   ├── dto
    │   │   ├── enums
    │   │   ├── exception
    │   │   └── kakao
    │   │       └── dto
    │   ├── dto
    │   ├── enums
    │   ├── jwt
    │   │   ├── controller
    │   │   ├── dto
    │   │   ├── enums
    │   │   ├── exception
    │   │   └── service
    │   ├── redis
    │   │   └── util
    │   └── security
    ├── config
    ├── entity
    ├── exception
    ├── infrastructure
    │   ├── s3
    │   │   ├── controller
    │   │   ├── dto
    │   │   ├── enums
    │   │   ├── exception
    │   │   ├── service
    │   │   └── validation
    │   └── sse
    │       ├── controller
    │       ├── domain
    │       ├── handler
    │       ├── repository
    │       ├── scheduler
    │       └── service
    ├── resolver
    ├── response
    │   ├── base
    │   ├── code
    │   └── dto
    └── util
```
