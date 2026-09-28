# 🔄 UNIFORME CIRCULAR - UPN

![Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF.svg?style=flat&logo=kotlin&logoColor=white)
![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28.svg?style=flat&logo=firebase&logoColor=black)
![UPN](https://img.shields.io/badge/Institution-UPN-002F6C.svg?style=flat)

**Uniforme Circular** es una solución móvil nativa diseñada exclusivamente para la comunidad estudiantil de la **Universidad Privada del Norte (UPN)**. Su objetivo principal es fomentar la economía circular y la sostenibilidad textil, permitiendo a los alumnos donar, intercambiar o transferir prendas de sus respectivos uniformes institucionales (carreras de Ingeniería, Salud, Derecho, Arquitectura, Negocios y Comunicaciones) mediante un sistema equitativo de puntos de impacto circular.

---

## ✨ Características Principales

### 🔐 1. Autenticación Institucional Segura
* **Login Automatizado:** Optimizado para la experiencia del estudiante. El usuario ingresa únicamente su código/identificador de alumno y la aplicación añade automáticamente la extensión corporativa `@upn.pe`, convirtiendo todo a minúsculas en tiempo real.
* **Control de Errores Firebase:** Los flujos de excepción distinguen explícitamente entre un usuario no registrado (`username_incorrect_error`) y una contraseña inválida (`password_incorrect_error`) en forma de Toasts individuales y altamente descriptivos.
* **Registro Riguroso:** Validación obligatoria que exige el ingreso de nombres y apellidos completos (mínimo dos palabras separadas por espacio) para garantizar la veracidad de la red de usuarios y la seguridad de los intercambios en el campus.

### 👕 2. Formulario de Aporte Inteligente (Deliver)
* **Validación Visual Interactiva con Auto-Scroll:** Si al subir un uniforme se olvida adjuntar la fotografía o seleccionar alguna opción vital de los `ChipGroups` (Talla, Género, Condición Física de la prenda, o Modalidad), la aplicación ejecuta automáticamente un **desplazamiento visual suave (`smoothScrollTo`)** directo a la sección o etiqueta correspondiente.
* **Feedback Dinámico de Errores:** Las etiquetas huérfanas se tiñen instantáneamente de color rojo de advertencia (`holo_red_dark`) y restauran su tonalidad original de inmediato en el momento exacto en que el usuario pulsa y selecciona una opción válida (`setOnCheckedChangeListener`).
* **Asignación Automática de Puntos:** Los puntos circulares asignados a la prenda se calculan de manera transparente según el estado de conservación física seleccionado por el estudiante, promoviendo el cuidado de las prendas.

### 🔍 3. Catálogo Dinámico y Visor Multimedia Avanzado
* **Filtros por Facultad y Metadatos:** Navegación optimizada mediante pestañas horizontales de Material Chips para segmentar prendas por carreras académicas, géneros y tallas rápidamente.
* **Pinch-to-Zoom y Arrastre Nativo:** La sección **Detalle de la Prenda** permite abrir la imagen a pantalla completa con soporte multimedia avanzado: **Zoom de 1x a 5x** usando dos dedos (Pinch) y **Arrastre libre (Pan)** con un dedo para inspeccionar a máximo detalle el estado textil, logos bordados e integridad de la prenda.
* **Reseteo Animado Inteligente:** Al disminuir el zoom o levantar los dedos de la pantalla, la imagen retorna a su posición original centrada de forma fluida mediante animaciones dinámicas del sistema.

### 📊 4. Resumen de Impacto Circular ("Mi Impacto")
* **Estadísticas en Tiempo Real:** Un espacio personalizado en un BottomSheet que cuantifica las prendas aportadas y los puntos acumulados por el alumno en la red.
* **Botón de Refresco Continuo (`btnRefreshImpact`):** Implementado en la cabecera mediante un contenedor `RelativeLayout` que permite realizar consultas asíncronas directas a Cloud Firestore para actualizar los marcadores al instante, acompañado de una **animación de rotación completa de 360° (`RotateAnimation`)** fluida.

### 🛡️ 5. Módulo de Administración Integrado (Roles)
* **Supervisión de Publicaciones:** Los usuarios administradores disponen de un menú emergente exclusivo para moderar, observar o dar de baja publicaciones directamente desde la interfaz del catálogo.
* **Flujo de Observación Avanzado:** Al sancionar u observar una publicación, el mensaje de error se proyecta sobre el contenedor `TextInputLayout`, expandiendo el aviso descriptivo automáticamente abajo en letras rojas sin requerir clics adicionales. Al redactar el motivo en el `TextInputEditText`, el error visual desaparece automáticamente en tiempo real.

---

## 🛠️ Stack Tecnológico & Dependencias

* **Lenguaje:** [Kotlin](https://kotlinlang.org/) (100% Desarrollo Nativo Android)
* **Arquitectura:** Arquitectura orientada a componentes de interfaz de usuario con Vistas XML, Material Design y enlace de datos dinámico.
* **Base de Datos & Backend:** 
  * **Firebase Authentication:** Gestión y autenticación segura de cuentas estudiantiles.
  * **Cloud Firestore:** Almacenamiento no relacional y transaccional para prendas, perfiles de usuarios y estados de reserva en tiempo real.
* **Librerías Clave:**
  * `androidx.core:core-ktx:1.13.1`
  * `com.google.android.material:material:1.12.0` (Uso extendido de Chips, TextInputLayout y BottomSheetDialogs).
  * `com.github.bumptech.glide:glide:4.16.0` (Carga asíncrona y optimización en caché de imágenes remotas y decodificación en Base64).

---

## 🚀 Instalación y Configuración Rápida

Sigue estos pasos para compilar y ejecutar el proyecto localmente:

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/TuUsuarioDeGitHub/Uniforme-Circular.git
   ```

2. **Agregar las credenciales de Firebase:**
   * Regístrate en la consola de Firebase y crea un nuevo proyecto Android con el paquete de la aplicación (`com.example.uniformecircular`).
   * Descarga tu archivo `google-services.json`.
   * Colócalo en el directorio raíz del módulo de la aplicación: `/app/google-services.json`.

3. **Sincronizar e Iniciar en Android Studio:**
   * Abre Android Studio e importa la carpeta raíz del proyecto.
   * Ejecuta un **Gradle Sync** para descargar todas las dependencias automáticas.
   * Compila e instala la aplicación en un emulador o dispositivo físico con Android 7.0 (API 24) o superior.

---

## ♻️ Filosofía del Proyecto

**Uniforme Circular** busca reducir significativamente la huella ecológica textil generada año tras año en los campus universitarios de la **UPN**, otorgando una segunda vida útil a las batas de laboratorio, chompas institucionales y casacas deportivas. Transforma el compromiso medioambiental en una práctica digital cotidiana, segura, interactiva y gratificante mediante la tecnología móvil.
