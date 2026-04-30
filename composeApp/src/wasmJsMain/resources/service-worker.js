const CACHE_NAME = 'libretapp-v1';
const ASSETS_TO_CACHE = [
  './',
  './index.html',
  './composeApp.js',
  './styles.css',
  './manifest.json',
  './sqldelight-worker.js'
];

// Instalar el Service Worker y cachear los assets estáticos
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      console.log('SW: Cacheando assets principales');
      return cache.addAll(ASSETS_TO_CACHE);
    })
  );
});

// Activar el SW y limpiar caches antiguos
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames.map((cache) => {
          if (cache !== CACHE_NAME) {
            console.log('SW: Borrando cache antiguo:', cache);
            return caches.delete(cache);
          }
        })
      );
    })
  );
});

// Estrategia: Network First, falling back to cache
// Esto asegura que los usuarios siempre tengan la última versión si hay internet,
// pero puedan usar la app offline.
self.addEventListener('fetch', (event) => {
  // Solo interceptar peticiones GET
  if (event.request.method !== 'GET') return;

  event.respondWith(
    fetch(event.request)
      .then((response) => {
        // Clonar y guardar en cache si es una respuesta válida
        const responseClone = response.clone();
        caches.open(CACHE_NAME).then((cache) => {
          cache.put(event.request, responseClone);
        });
        return response;
      })
      .catch(() => {
        // Si falla la red, intentar buscar en cache
        return caches.match(event.request);
      })
  );
});
