# 🎨 Documentación Técnica de Front-end - CatalogoApp

Este documento sirve como la "fuente de verdad" para la identidad visual y la arquitectura de la interfaz de usuario de la aplicación. Está diseñado para que cualquier desarrollador o IA pueda retomar el proyecto y mantener la coherencia estética y técnica.

## 🎯 Visión del Diseño: "Retail Professional"
La aplicación ha evolucionado de un diseño genérico basado en plantillas hacia una identidad de **Retail de Alta Gama**, inspirada en apps como **Adidas, Temu y AliExpress**.

**Objetivo Principal:** El producto es el protagonista. Se elimina el "ruido" visual para centrarse en la conversión y la facilidad de uso.

---

## 💎 Sistema de Diseño (Design System)

### 1. Paleta de Colores (Sincronizada en `Theme.kt`)
| Elemento | Color | Código Hex | Función |
| :--- | :--- | :--- | :--- |
| **Primario** | Rosa Intenso | `#D81B60` | Call to Action (CTAs), botones principales, acentos. |
| **Contraste** | Azul Marino | `#1A237E` | Textos principales, iconos de navegación, estructura. |
| **Fondo** | Gris Muy Claro | `#F5F5F5` | Fondo general para evitar fatiga visual. |
| **Superficie** | Blanco Puro | `#FFFFFF` | Tarjetas, contenedores y fondos de inputs. |
| **Soporte** | Gris Neutro | `#BDBDBD` | Bordes finos y líneas divisorias. |

### 2. Reglas de Estética (UX/UI)
- **Bordes:** Uso de `RoundedCornerShape` consistentes. Tarjetas con bordes suaves (`12.dp` a `24.dp`).
- **Sombras:** Eliminación de elevaciones pesadas. Se prefieren bordes finos (`1.dp`) o sombras muy tenues para un look "flat" moderno.
- **Espaciado:** Uso de `Padding` generoso para crear "aire" y evitar la saturación de información.
- **Tipografía:** Jerarquía basada en pesos (`ExtraBold` para títulos, `Medium` para etiquetas, `Regular` para descripciones).

---

## 🚀 Implementaciones Realizadas

### 📱 Estructura Global y Navegación
- **Dynamic Status Bar:** Configuración de contraste automático. Barra de estado oscura/transparente con iconos blancos para visibilidad total de batería y hora.
- **Bottom Navigation Bar:** Barra inferior fija con iconos minimalistas (`Inicio`, `Catálogo`, `Pedidos`, `Perfil`).
- **Contenedor Principal:** Uso de `MainScreen` como wrapper para mantener la barra inferior persistente.

### 🖼️ Pantallas Rediseñadas
- **Login & Registro:** Diseño de "Tarjeta Flotante" sobre fondo limpio. Botones estilo cápsula.
- **HomeScreen:** Dashboard con saludo personalizado, tarjeta de progreso (gamificación) y Grid de acciones rápidas.
- **Catálogo:**
    - Transformación de Lista $\rightarrow$ **Cuadrícula (Grid) de 2 columnas**.
    - Implementación de **Buscador en tiempo real**.
    - Eliminación de elementos distractores (como el botón de devolución).
    - Enfoque total en la imagen y el precio del producto.
- **Perfil (Rediseño Minimalista):** 
    - Transformación de formulario en **Dashboard de Identidad**.
    - Diseño asimétrico alineado al inicio.
    - Separación de vista de perfil y pantalla de edición (`EditProfileScreen`) para transiciones fluidas.
    - Implementación de panel de KPIs (Ventas, Pedidos, Entregados) con diseño flat.

---

## 🛠️ Detalles Técnicos para Futuras Iteraciones

### Manejo de Estado y UI
- **Architecture:** MVVM (Model-View-ViewModel).
- **UI Framework:** Jetpack Compose.
- **Navigation:** `NavHost` con rutas definidas en el objeto `Rutas`.
- **System UI:** Control de `WindowInsetsController` para manejar la visibilidad de los iconos del sistema.

---

## 📅 Hoja de Ruta (Pendientes de Front-end)

### 🔴 Prioridad Alta
- [ ] **Identidad Profesional del Perfil:**
    - Implementar campos reales de identidad en Firebase y UI (`Nombre`, `Apellido`, `Documento`, `Teléfono`, `Dirección`, `Ciudad`).
    - Eliminar campos vagos como "Preferencias/Meta".
- [ ] **Soporte y Ajustes:**
    - Crear pantalla de **Centro de Ayuda (FAQ)** con acordeones de dudas comunes.
    - Crear pantalla de **Términos y Condiciones** con lectura de documento formal.


### 🟡 Prioridad Media
- [ ] **Animaciones de Transición:** Implementar `AnimatedContent` o `Shared Element Transitions` para que el paso entre pantallas sea más fluido.
- [ ] **Filtros Avanzados:** Implementar filtros por rango de precio y ordenamiento (menor a mayor).

### 🟢 Mejoras Continuas
- [ ] **Modo Oscuro (Dark Mode):** Refinar la paleta de colores para el modo noche basándose en el Azul Marino.
- [ ] **Micro-interacciones:** Añadir efectos de escala al presionar los botones de productos.
