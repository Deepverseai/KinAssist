FROM node:20-alpine

WORKDIR /app

COPY package.json ./
RUN npm install --omit=dev

COPY index.js ./

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/health || exit 1

CMD ["node", "index.js"]
