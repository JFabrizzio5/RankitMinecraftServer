FROM node:18-alpine

WORKDIR /usr/src/app

# Copy dependency manifests
COPY package*.json ./

# Install clean production dependencies
RUN npm install --omit=dev

# Copy application source code
COPY . .

# Expose port 3000
EXPOSE 3000

# Start server
CMD ["node", "server.js"]
