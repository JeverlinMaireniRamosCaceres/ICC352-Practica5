let socketAdmin;
let chatActivo = null;
let conversaciones = {};

document.addEventListener('DOMContentLoaded', () => {
    socketAdmin = new WebSocket('ws://' + location.hostname + ':' + location.port + '/admin-chat');

    socketAdmin.onopen = function () {
        socketAdmin.send(JSON.stringify({ tipo: 'cargarChats' }));
    };

    socketAdmin.onmessage = function (event) {
        const data = JSON.parse(event.data);

        if (data.tipo === 'nuevoUsuario') {
            if (!conversaciones[data.sessionId]) {
                conversaciones[data.sessionId] = {
                    nombre: data.nombre,
                    mensajes: [],
                    mensajesNuevos: 0
                };
            }
            renderListaUsuarios();
        }

        if (data.tipo === 'mensaje') {
            if (!conversaciones[data.sessionId]) {
                conversaciones[data.sessionId] = {
                    nombre: data.nombre,
                    mensajes: [],
                    mensajesNuevos: 0
                };
            }
            conversaciones[data.sessionId].mensajes.push({
                emisor: 'usuario',
                texto: data.mensaje,
                timestamp: Date.now()
            });
            if (chatActivo !== data.sessionId) {
                conversaciones[data.sessionId].mensajesNuevos++;
            }
            renderListaUsuarios();

            if (chatActivo === data.sessionId) {
                renderMensajes(chatActivo);
            }
        }

        if (data.tipo === 'historial') {
            if (!conversaciones[data.sessionId]) {
                conversaciones[data.sessionId] = {
                    nombre: data.nombre,
                    mensajes: [],
                    mensajesNuevos: 0
                };
            }
            conversaciones[data.sessionId].mensajes.push({
                emisor: data.emisor,
                texto: data.mensaje
            });
            renderListaUsuarios();
            if (chatActivo === data.sessionId) {
                renderMensajes(chatActivo);
            }
        }

        if (data.tipo === 'usuarioDesconectado') {
            delete conversaciones[data.sessionId];
            if (chatActivo === data.sessionId) {
                chatActivo = null;
                document.getElementById('nombre-chat-activo').textContent = 'Selecciona una conversación';
                document.getElementById('mensajes-panel').innerHTML =
                    '<p class="text-muted text-center">Selecciona una conversación para ver los mensajes</p>';
            }
            renderListaUsuarios();
        }
    };
});

function renderListaUsuarios() {
    const lista = document.getElementById('lista-usuarios');
    const contador = document.getElementById('contador-chats');

    const ids = Object.keys(conversaciones).sort((a, b) => {
        const mensajesA = conversaciones[a].mensajes;
        const mensajesB = conversaciones[b].mensajes;
        const ultimoA = mensajesA.length > 0 ? mensajesA[mensajesA.length - 1].timestamp || 0 : 0;
        const ultimoB = mensajesB.length > 0 ? mensajesB[mensajesB.length - 1].timestamp || 0 : 0;
        return ultimoB - ultimoA;
    });

    contador.textContent = ids.length;

    if (ids.length === 0) {
        lista.innerHTML = `<p class="text-muted text-center p-3">No hay conversaciones activas</p>`;
        return;
    }

    lista.innerHTML = '';

    ids.forEach(sessionId => {
        const item = document.createElement('div');
        item.className = 'usuario-item' + (chatActivo === sessionId ? ' activo' : '');
        const tieneNuevos = conversaciones[sessionId].mensajesNuevos > 0;
        item.innerHTML = `
        ${conversaciones[sessionId].nombre}
        ${tieneNuevos ? '<span class="punto-nuevo"></span>' : ''}
    `;
        item.onclick = () => seleccionarChat(sessionId);
        lista.appendChild(item);
    });
}

function seleccionarChat(sessionId) {
    chatActivo = sessionId;
    conversaciones[sessionId].mensajesNuevos = 0;
    document.getElementById('nombre-chat-activo').textContent = conversaciones[sessionId].nombre;
    renderListaUsuarios();
    renderMensajes(sessionId);
}

function renderMensajes(sessionId) {
    const panel = document.getElementById('mensajes-panel');
    const mensajes = conversaciones[sessionId].mensajes;

    if (!mensajes.length) {
        panel.innerHTML = `<p class="text-muted text-center">No hay mensajes todavía</p>`;
        return;
    }

    panel.innerHTML = '';

    mensajes.forEach(msg => {
        const div = document.createElement('div');
        div.classList.add('mb-2');

        if (msg.emisor === 'usuario') {
            div.innerHTML = `
                <div class="text-start">
                    <span class="badge" style="background-color: #B19CD9;">Usuario</span>
                    <div class="bg-white border rounded p-2 d-inline-block">${msg.texto}</div>
                </div>
            `;
        } else {
            div.innerHTML = `
                <div class="text-end">
                    <span class="badge" style="background-color: #6A5ACD;">Admin</span>
                    <div class="rounded p-2 d-inline-block text-white" style="background-color: #6A5ACD;">${msg.texto}</div>
                </div>
            `;
        }

        panel.appendChild(div);
    });

    panel.scrollTop = panel.scrollHeight;
}

function responderMensaje() {
    const input = document.getElementById('input-respuesta');
    const texto = input.value.trim();

    if (!chatActivo || !texto || !socketAdmin) {
        return;
    }

    socketAdmin.send(JSON.stringify({
        sessionId: chatActivo,
        mensaje: texto
    }));

    conversaciones[chatActivo].mensajes.push({
        emisor: 'admin',
        texto: texto,
        timestamp: Date.now()
    });

    renderMensajes(chatActivo);
    input.value = '';
}