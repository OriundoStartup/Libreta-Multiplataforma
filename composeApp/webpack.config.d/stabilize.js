;(function(config) {
    // 1. Dev Server Headers (COOP/COEP)
    if (config.devServer) {
        config.devServer.headers = {
            ...config.devServer.headers,
            "Cross-Origin-Opener-Policy": "same-origin",
            "Cross-Origin-Embedder-Policy": "require-corp"
        };
    }
})(config);
