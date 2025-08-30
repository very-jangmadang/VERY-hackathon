import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react-swc';
import svgr from '@svgr/rollup';
import { nodePolyfills } from 'vite-plugin-node-polyfills';

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    svgr(),
    nodePolyfills({
      // crypto와 buffer를 포함한 Node.js 전역 및 내장 모듈에 대한 폴리필을 주입합니다.
      protocolImports: true,
    }),
  ],
  define: {
    // Wepin SDK와 같은 일부 라이브러리는 'global' 객체를 사용하므로,
    // 브라우저의 'globalThis'를 'global'로 정의해줍니다.
    global: 'globalThis',
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          // 큰 SVG 파일들을 별도 청크로 분리하여 Babel 경고 방지
          svgAssets: [
            './src/assets/homePage/promotion1.svg',
            './src/assets/homePage/promotion2.svg',
            './src/assets/homePage/promotion3.svg',
          ],
        },
      },
    },
    // 빌드 최적화 설정
    chunkSizeWarningLimit: 2000, // 청크 크기 경고 임계값 증가
  },
  // SVG 최적화 설정
  optimizeDeps: {
    exclude: [
      './src/assets/homePage/promotion1.svg',
      './src/assets/homePage/promotion2.svg',
      './src/assets/homePage/promotion3.svg',
    ],
  },
  server: {
    proxy: {
      '/api': {
        target: 'https://jangmadang.site',
        changeOrigin: true,
        secure: true,
        configure: (proxy, options) => {
          proxy.on('proxyReq', (proxyReq, req, res) => {
            // 크로스도메인 요청을 위한 헤더 설정
            proxyReq.setHeader('X-Forwarded-Host', req.headers.host);
            proxyReq.setHeader('X-Forwarded-Proto', 'https');
          });
        },
      },
    },
  },
});
