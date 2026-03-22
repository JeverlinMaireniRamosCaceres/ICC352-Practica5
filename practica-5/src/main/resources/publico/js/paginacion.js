let paginaActual = 1;

function cargarPagina(page) {
    fetch(`/articulos/pagina?page=${page}`)
        .then(res => res.json())
        .then(data => {
            paginaActual = data.page;

            // Actualizar indicador
            document.getElementById('indicador-pagina').textContent =
                `Página ${data.page} de ${data.totalPages}`;

            // Botón anterior
            const btnAnterior = document.getElementById('btn-anterior');
            btnAnterior.classList.toggle('disabled', data.page === 1);

            // Botón siguiente
            const btnSiguiente = document.getElementById('btn-siguiente');
            btnSiguiente.classList.toggle('disabled', data.page === data.totalPages);

            // Renderizar tarjetas
            const contenedor = document.getElementById('contenedor-articulos');
            contenedor.innerHTML = '';

            data.articulos.forEach(articulo => {
                const etiquetasHtml = articulo.etiquetas
                    .map(e => `<span class="badge me-1">${e}</span>`)
                    .join('');

                const fecha = new Date(articulo.fecha);
                const fechaFormateada = fecha.toLocaleString('es-ES', {
                    day: '2-digit', month: '2-digit', year: 'numeric',
                    hour: '2-digit', minute: '2-digit'
                });

                contenedor.innerHTML += `
                    <div class="col-md-6">
                        <div class="card flex-md-row mb-4 box-shadow h-md-250">
                            <div class="card-body d-flex flex-column align-items-start">
                                <div class="mb-2">${etiquetasHtml}</div>
                                <h3 class="mb-0">
                                    <a class="text-dark" href="/articulos/${articulo.id}">
                                        ${articulo.titulo}
                                    </a>
                                </h3>
                                <div class="mb-1 text-muted">${fechaFormateada}</div>
                                <p class="card-text mb-auto text-break">${articulo.cuerpo}</p>
                                <a href="/articulos/${articulo.id}">Continuar la lectura</a>
                            </div>
                        </div>
                    </div>
                `;
            });
        });
}

function cambiarPagina(page) {
    cargarPagina(page);
}

document.addEventListener('DOMContentLoaded', () => cargarPagina(1));