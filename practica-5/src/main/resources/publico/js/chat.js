let webSocket;
let nombreUsuario = '';

function abrirChat() {
    document.getElementById('btn-abrir-chat').style.display = 'none';
    document.getElementById('ventana-chat').style.display = 'block';
}

function cerrarChat() {
    document.getElementById('btn-abrir-chat').style.display = 'block';
    document.getElementById('ventana-chat').style.display = 'none';
}

function iniciarChat() {
    const nombre = document.getElementById('input-nombre').value.trim();
    if (!nombre) {
        alert('Por favor ingresa tu nombre');
        return;
    }

    nombreUsuario = nombre;

    webSocket = new WebSocket('ws://' + location.hostname + ':' + location.port + '/chat');

    webSocket.onopen = function () {
        webSocket.send(JSON.stringify({ tipo: 'inicio', nombre: nombreUsuario }));

        document.getElementById('paso-nombre').classList.add('d-none');
        document.getElementById('paso-chat').classList.remove('d-none');

        agregarMensaje('Conectado. Espera a que un administrador responda.', 'sistema');
    };

    webSocket.onmessage = function (event) {
        const data = JSON.parse(event.data);
        if (data.tipo === 'mensajeAdmin') {
            agregarMensaje(data.mensaje, 'admin');
        }
    };

    webSocket.onclose = function () {
        agregarMensaje('Conexión cerrada.', 'sistema');
    };

    webSocket.onerror = function () {
        agregarMensaje('Error de conexión.', 'sistema');
    };
}

function enviarMensaje() {
    const input = document.getElementById('input-mensaje');
    const texto = input.value.trim();
    if (!texto || !webSocket) return;

    webSocket.send(JSON.stringify({ tipo: 'mensaje', nombre: nombreUsuario, mensaje: texto }));
    agregarMensaje(texto, 'usuario');
    input.value = '';
}

function agregarMensaje(texto, tipo) {
    const contenedor = document.getElementById('mensajes-chat');
    const div = document.createElement('div');
    div.classList.add('mb-2');

    if (tipo === 'usuario') {
        div.innerHTML = `<div class="text-end">
        <span class="badge" style="background-color: #6A5ACD;">${nombreUsuario}</span>
        <div class="rounded p-2 d-inline-block text-white" style="background-color: #6A5ACD;">${texto}</div>
    </div>`;
    } else if (tipo === 'admin') {
        div.innerHTML = `<div class="text-start">
        <span class="badge" style="background-color: #B19CD9;">Admin</span>
        <div class="bg-white border rounded p-2 d-inline-block" style="border-color: #B19CD9 !important;">${texto}</div>
    </div>`;
    } else {
        div.innerHTML = `<div class="text-center small" style="color: #6A5ACD;">${texto}</div>`;
    }

    contenedor.appendChild(div);
    contenedor.scrollTop = contenedor.scrollHeight;
}