# CatalogoApp — Avance Sprint 1 (HU01 + HU02)

Proyecto de Android Studio listo para abrir. Implementa:
- **HU01** — Registro e inicio de sesión de consultoras (Firebase Auth)
- **HU02** — Gestión de perfil de consultora (nombre, foto con cámara, preferencias → Firestore + WorkManager)

---

## 1. Antes de abrir el proyecto: crear tu Firebase (15 min, hazlo HOY)

El proyecto no puede compilar sin esto — el login/registro necesita un backend real de Firebase.

1. Ve a [console.firebase.google.com](https://console.firebase.google.com) → **Crear proyecto** → nómbralo `CatalogoApp` (o el que quieras).
2. Dentro del proyecto, click en el ícono de Android → **Agregar app Android**.
   - Nombre del paquete (obligatorio, debe ser EXACTO): `com.catalogoapp.consultoras`
3. Descarga el archivo **`google-services.json`** que te ofrece.
4. Cópialo dentro de la carpeta `app/` de este proyecto (al mismo nivel que `app/build.gradle.kts`).
5. En la consola de Firebase, ve a **Authentication → Sign-in method** → habilita **Correo electrónico/contraseña**.
6. Ve a **Firestore Database → Crear base de datos** → elige **modo de prueba** (test mode) para que no te bloquee por reglas de seguridad durante la demo.

Sin los pasos 5 y 6, el registro y el guardado de perfil van a fallar aunque el código esté bien.

## 2. Abrir y correr

1. Abre Android Studio → **Open** → selecciona la carpeta `CatalogoApp`.
2. Si te pide actualizar el Gradle Wrapper o regenerar `gradlew`, acepta (el proyecto no trae el binario `gradle-wrapper.jar`, Android Studio lo genera solo).
3. Espera el **Gradle Sync** (barra de abajo). Si marca error de versión, dale a "Sync Now" de nuevo o acepta las sugerencias de Android Studio.
4. Crea o abre un emulador (**Device Manager**) con API 26 o superior.
5. Run ▶️.

## 3. Antes de la demo en vivo: probar una vez

**Importante:** la verificación de correo es real (Firebase manda un correo de verdad). Para no depender de revisar un correo en vivo frente a la profesora:

1. Un rato antes de tu turno, registra una cuenta de prueba con un correo tuyo real.
2. Abre ese correo y haz click en el link de verificación.
3. Cierra sesión en la app.
4. En la demo, solo haces **login** con esa cuenta ya verificada — así evitas el tiempo muerto de "revisar el correo" en vivo.
5. Si quieres mostrar el registro también, hazlo con OTRO correo de prueba y explica: "aquí llega el correo de verificación" (sin necesidad de abrirlo en vivo).

## 4. Guion para la presentación (según lo que pidió Ángel: una funcionalidad, lado usuario + código, sin florear)

**No expliques:** qué es un import, qué es un String, cómo se define una función. Ve directo a la lógica de negocio.

### Parte 1 — Lado usuario (emulador), ~1 min
1. Abre la app → pantalla de Login.
2. Toca "¿No tienes cuenta? Regístrate" → llena email + contraseña → Crear cuenta.
3. Di: *"Esto crea el usuario en Firebase Authentication y le manda un correo de verificación."*
4. Vuelve a Login con la cuenta ya verificada de antemano.
5. Llega a Perfil → toca "Tomar foto con la cámara" (el emulador simula la cámara) → llena nombre y preferencias → Guardar perfil.
6. Di: *"Esto guarda el perfil en Firestore, y dispara una sincronización en segundo plano con WorkManager, sin bloquear la pantalla."*
7. Cierra la app y vuelve a abrirla: entra directo a Perfil sin pedir login de nuevo. Di: *"La sesión queda persistida — Firebase Auth no pide loguearse de nuevo."*

### Parte 2 — El código (2-3 min), muestra estos 3 archivos nada más
1. **`AuthRepository.kt`** — *"Aquí está toda la lógica de autenticación separada de la pantalla: registrar, iniciar sesión, recuperar contraseña. Cada función llama al SDK de Firebase Auth y devuelve éxito o error."* (Señala `registrar()`: crea el usuario y manda la verificación en la misma función).
2. **`AuthViewModel.kt`** — *"Este es el ViewModel del patrón MVVM: la pantalla (Compose) nunca habla directo con Firebase, solo observa este estado (`uiState`) y llama a estas funciones. Así separamos la interfaz de la lógica."*
3. **`ProfileViewModel.kt`**, función `guardarPerfil` — *"Guarda el perfil en Firestore y, si sale bien, encola el `ProfileSyncWorker` con WorkManager para la sincronización en segundo plano."*
   
**Si preguntan "¿por qué así?":** MVVM separa responsabilidades (pantalla / lógica / datos) — es el patrón pedido en el backlog para todo el proyecto, no solo esta historia.

---

## Estructura del proyecto

```
app/src/main/java/com/catalogoapp/consultoras/
├── MainActivity.kt              → arranca la app y la navegación
├── navigation/NavGraph.kt       → rutas entre pantallas
├── ui/screens/auth/             → Login, Registro, Recuperar contraseña
├── ui/screens/profile/          → Perfil (cámara + Firestore)
├── viewmodel/                   → AuthViewModel, ProfileViewModel (MVVM)
├── data/repository/             → AuthRepository, ProfileRepository (hablan con Firebase)
├── data/model/                  → ConsultoraProfile (modelo de Firestore)
└── worker/ProfileSyncWorker.kt  → sincronización en segundo plano
```

## Qué falta para el resto de HU02 (no incluido aquí, para no sobrecargar esta entrega)
- Subir la foto real a Firebase Storage (aquí solo se muestra localmente, ya que Storage necesita configuración y reglas aparte).
- Pantalla de edición de preferencias más elaborada (categorías en vez de texto libre).
