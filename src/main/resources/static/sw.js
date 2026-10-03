// 本文件不注册任何 Service Worker。
// 仅用于：当某个旧站点在此 origin 注册过 /sw.js 时，浏览器检查更新会取到这个文件，
// 新 SW 激活后立即注销自己，从而清除残留的旧 Service Worker。
self.addEventListener('install', () => self.skipWaiting());

self.addEventListener('activate', (event) => {
  event.waitUntil((async () => {
    try {
      await self.registration.unregister();
      const clients = await self.clients.matchAll({ type: 'window' });
      clients.forEach((c) => c.navigate(c.url));
    } catch (e) {
      // ignore
    }
  })());
});
