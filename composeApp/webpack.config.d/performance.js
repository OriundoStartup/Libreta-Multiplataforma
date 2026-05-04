// Desactivar advertencias de rendimiento (tamaño de assets) que son comunes en proyectos Compose Wasm
config.performance = {
    hints: false
};

// Evitar la advertencia de que "hot: true" no es recomendado para Wasm
if (config.devServer) {
    config.devServer.hot = false;
}

// Silenciar el aviso "Critical dependency: the request of a dependency is an expression"
// Muy común en librerías KMP que usan imports dinámicos
config.module = config.module || {};
config.module.exprContextCritical = false;
