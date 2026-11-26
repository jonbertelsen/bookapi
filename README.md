# Book API

Et lokalt biblioteks-API med Javalin 7.3.0, Hibernate/JPA 6.2.4.Final og PostgreSQL. Package-struktur og Javalin-initialisering følger [hotel_api_2026](https://github.com/jonbertelsen/hotel_api_2026). JSON-formatet følger `db.json` i [bookdemo, branchen router](https://github.com/jonbertelsen/bookdemo/tree/router).

## Start

Brug Java 25 og Maven. PostgreSQL skal køre på `localhost:5432`, og databasen `library` skal eksistere. Brugernavn og adgangskode er begge `postgres`; forbindelsen konfigureres i `app.config.HibernateConfig`.

Kør `app.Main` i IntelliJ, eller byg og start fra terminalen:

```sh
mvn package
java -jar target/app.jar
```

API'et lytter kun på localhost: `http://localhost:7070/api/v1`. Routeoversigten findes på `http://localhost:7070/api/v1/routes`.

Hibernate opretter tabellerne `book`, `member`, `loan`, `role` og `member_role`. `hbm2ddl.auto=update` bevarer data mellem genstarter. Ved første opstart importeres hele den oprindelige `db.json` fra `src/main/resources`, hvis alle tre tabeller er tomme. Seed-data indeholder allerede ét aktivt lån på bog `1`.

## Tilslut React

I frontendens `src/api.js` erstattes `BASE_URL` med:

```js
const BASE_URL = 'http://localhost:7070/api/v1'
```

Start React med `npm run dev`; JSON Server skal ikke længere startes. CORS tillader `http://localhost:5173` og `http://127.0.0.1:5173`. Hvis Vite bruger en anden port, tilføjes den i `ApplicationConfig`.

Frontendens fejltekst i `src/layouts/LibraryLayout.jsx` omtaler stadig JSON Server på port 4000; ret den til Javalin på port 7070. Frontend-repositoriet er ikke ændret her.

## Login og JWT

Start `app.Main` igen efter opdateringen. Medlemmer har et unikt `username` og et bcrypt-hashet kodeord. Eksisterende medlemmer får deres medlems-id som login og standardkodeordet `1234`:

| Medlem | Username | Kodeord | Rolle |
| --- | --- | --- | --- |
| Alex Jensen | `1` | `1234` | `user` |
| Sam Nielsen | `2` | `1234` | `admin` |

```http
POST http://localhost:7070/api/v1/auth/login
Content-Type: application/json

{"username": "1", "password": "1234"}
```

Svaret har formen:

```json
{
  "token": "<JWT>",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "member": {"id": "1", "name": "Alex Jensen", "username": "1", "roles": ["user"]}
}
```

Tokenet er gyldigt i én time og indeholder medlems-id som `sub`, username, medlemmets roller fra databasen (`user` og/eller `admin`), issuer, audience, udstedelsestid og udløbstid. Serveren kontrollerer signatur og claims. Kodeord og hashes returneres ikke.

Brug tokenet på det beskyttede endpoint:

```http
GET http://localhost:7070/api/v1/auth/me
Authorization: Bearer <JWT>
```

`/auth/me` returnerer det indloggede medlem. Manglende, ugyldigt eller udløbet token giver 401. Et slettet medlems token afvises også. Forkert login og ukendt username giver samme 401-fejl. Ugyldige request bodies giver 400.

De eksisterende `/books`, `/members` og `/loans` endpoints er fortsat offentlige; denne version tilføjer login og et beskyttet profil-endpoint. Frontenden kan bruge `member.id` fra login-svaret i stedet for sit faste `CURRENT_MEMBER_ID`. Der er endnu ikke adgangskontrol til de enkelte medlemmers lån eller et register-endpoint.

Ved `POST /members` kan `username` angives, eksempelvis `{"name":"Alex Jensen","username":"alex"}`. Udelades username, anvendes det genererede medlems-id. Nye medlemmer får også kodeordet `1234`. Navneændringer ændrer ikke kodeordet. Eksisterende hashes bevares ved genstart.

Roller gemmes i `role` med rollenavnet som primærnøgle. `member_role` kobler medlemmer og roller med en many-to-many-relation. Der oprettes som udgangspunkt `user` og `admin`; medlem 1 får `user`, medlem 2 får `admin`, og nye medlemmer får `user`. Ved genstart tildeles standardrollen kun til medlemmer uden roller, så senere rolletildelinger bevares.

`Member.addRole(Role)` og `Member.removeRole(String)` bruges til at ændre roller i en JPA-transaktion. Der er ikke tilføjet et endpoint til administration af roller. Feltet `roles` er kun til læsning i medlems-API'et; man kan ikke tildele sig selv `admin` via POST/PUT. Medlems-, login- og `/auth/me`-svar indeholder `roles` som en liste af navne.

`/auth/me` accepterer både `user` og `admin`. Filteret kontrollerer både rollerne i JWT og medlemmets aktuelle databaseroller. Log ind igen efter rolletildeling for at få et JWT med de nye roller. Rolleændringen begrænser ikke de eksisterende offentlige bog- og låneendpoints.

`app.config.SecurityConfig` udfylder kun manglende credentials ved opstart og sætter databasekolonnerne til NOT NULL. Username er unikt. `app.security.PasswordService` bruger jBCrypt med salt og cost 12.

JWT-indstillingerne findes i `src/main/resources/security.properties`. Filen indeholder en fast nøgle til lokal demo, så tokens virker efter genstart. Miljøvariablen `JWT_SECRET` kan tilsidesætte nøglen og skal være mindst 32 tegn.

Løsningen følger login/bcrypt/filter-opbygningen fra [kursets opskrift](https://3sem.kursusmaterialer.dk/toolbox/security/api-security/). JWT signeres og valideres med Auth0 `java-jwt` fra Maven Central.

## Endpoints

Alle stier har præfikset `/api/v1`.

| Metode | Sti | Handling |
| --- | --- | --- |
| POST | `/auth/login` | Login og JWT |
| GET | `/auth/me` | Indlogget medlem, kræver Bearer-token |
| GET | `/books`, `/members`, `/loans` | Hent liste |
| GET | `/books/{id}`, `/members/{id}`, `/loans/{id}` | Hent én |
| POST | `/books`, `/members`, `/loans` | Opret |
| PUT | `/books/{id}`, `/members/{id}`, `/loans/{id}` | Erstat felter |
| DELETE | `/books/{id}`, `/members/{id}`, `/loans/{id}` | Slet, svar 204 |
| PATCH | `/loans/{id}` | Aflever eller genåbn med `{"returned": true/false}` |

JSON-eksempler for oprettelse:

```json
{"title": "Refactoring", "author": "Martin Fowler"}
```

```json
{"name": "Alex Jensen"}
```

```json
{"bookId": "2", "memberId": "1", "dueDate": "2026-12-10", "returned": false}
```

Id'er er strenge, så frontendens sammenligninger og eksisterende id'er bevares. Nye id'er genereres som UUID-strenge. Lån har JPA-relationer til bog og medlem, men DTO'erne eksponerer fortsat `bookId` og `memberId`. Datoer returneres som `YYYY-MM-DD`.

En bog kan kun have ét aktivt lån. En databaselås på bogen beskytter mod samtidige udlån. Bøger og medlemmer med lånehistorik kan ikke slettes, før deres lån er slettet. Ugyldige input giver 400, ukendte id'er 404 og konflikter 409. Login udsteder JWT med medlemmets roller fra databasen (`user` og/eller `admin`).

HTTP-eksempler til IntelliJ findes i `src/main/resources/http/dev.http`.

## Struktur

`app.Main` starter Hibernate, importerer startdata og kalder `ApplicationConfig.startServer(7070)`. `ApplicationConfig` bruger `Javalin.create(ApplicationConfig::configuration)` og `Routes` med `EndpointGroup`, som referenceprojektet. Routes og exception handlers registreres i `config.routes`, og bannerindstillingen ligger i `config.startup`, jf. Javalin 7. Request bodies valideres med `.required()` før feltkontroller.

- `app.config`: Javalin, Hibernate og startdata.
- `app.routes`: routes samlet i `Routes` og én route-klasse pr. ressource.
- `app.controllers.impl`: request-validering og HTTP-svar via `IController`.
- `app.daos.impl`: databaseoperationer og transaktioner via `IDAO`.
- `app.entities`: JPA-entiteter.
- `app.dtos`: JSON-formatet til React.
- `app.exceptions`: fælles fejlformat.
- `app.security`: bcrypt, JWT og roller til Javalin-routes.

## Test

```sh
mvn test -Dlibrary.integration=true
```

De otte integrationstests kræver den lokale `library`-database. De starter en server på en ledig port og kontrollerer JSON-format, CRUD, lån/aflevering, dobbeltudlån, validering, CORS, login, bcrypt og JWT-adgang. Fire selvstændige JWT-tests kontrollerer claims, udløb og signatur uden database. Testoprettede poster slettes igen. Almindelig `mvn test` springer databaseintegrationstestene over.
