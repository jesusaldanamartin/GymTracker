# 🏋️ GymTracker

Una app Android para registrar y analizar tus entrenamientos. Sin suscripciones, sin anuncios, sin cuenta — tus datos se quedan en tu teléfono.

---

## ¿Qué hace?

GymTracker te permite llevar un diario de entrenamiento completo desde el móvil. Registras cada serie, cada repetición y cada peso, y la app se encarga de calcular métricas de progreso, detectar tendencias y mostrarte cómo evolucionas semana a semana.

---

## Funcionalidades

### 📋 Registro de entrenamientos
- Más de 40 ejercicios organizados por músculo y rutina (Push / Pull / Legs / Full / Cardio)
- Registro de series con reps, peso, variante de ejecución y nota
- Soporte para ejercicios de cardio con duración e intensidad
- Sesión en progreso que se guarda automáticamente si cierras la app
- Ejercicios personalizados — crea los tuyos con nombre, músculo, emoji y tipo

### 📈 Progreso y métricas
- **E1RM** (1 Rep Max estimado) para ejercicios de fuerza
- **Score de hipertrofia** (reps × peso) para ejercicios de volumen
- Gráficas de progresión por sesión — peso, volumen, reps, E1RM
- Detección automática de tendencia: progresando 🟢, mantenimiento 🟡, fatiga 🟠
- Historial completo por ejercicio y variante
- Card de consistencia semanal con racha y adherencia

### 🗓️ Historial
- Calendario mensual con sesiones coloreadas por tipo de rutina
- Vista detallada de cada sesión con todas las series
- Borrado de series o sesiones individuales

### 🔄 Exportar e importar
- Exporta tus datos a CSV con un toque — compatible con Excel y Google Sheets
- Importa CSVs de sesiones anteriores sin perder los datos existentes
- Fusión inteligente: no duplica series idénticas al importar

---

## Roadmap

Estas son las funcionalidades planificadas para las próximas versiones:

- [ ] **Análisis de técnica con YOLO** — análisis de la trayectoria de la barra en tiempo real usando la cámara
- [ ] **Planes de entrenamiento con IA** — generación de rutinas personalizadas mediante LLMs
- [ ] **Sincronización en la nube** — backup y acceso desde múltiples dispositivos
- [ ] **Widget para pantalla de inicio** — resumen rápido de la semana sin abrir la app

---

## Stack técnico

| Capa | Tecnología |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Arquitectura | MVVM + Clean Architecture |
| Base de datos | Room (SQLite local) |
| Inyección de dependencias | Hilt |
| Navegación | Navigation Compose |
| Persistencia de variantes | DataStore |
| Lenguaje | Kotlin |
| Build | Gradle KTS |

---

## Estructura del proyecto

```
app/
├── data/
│   ├── local/          # Room, Seeds, Storage legacy
│   └── repository/     # Implementaciones de repositorios
├── domain/
│   ├── model/          # Modelos de negocio
│   ├── repository/     # Interfaces
│   └── usecase/        # Casos de uso
├── presentation/
│   ├── components/     # Componentes compartidos, charts, dialogs
│   ├── navigation/     # Rutas de navegación
│   ├── screens/        # Una carpeta por pantalla
│   └── theme/          # Colores y tema
└── di/                 # Módulos de Hilt
```

---

## Privacidad

GymTracker no recopila ningún dato. No hay servidor, no hay cuenta, no hay telemetría. Todo se almacena localmente en tu dispositivo. Puedes exportar y borrar tus datos en cualquier momento.
