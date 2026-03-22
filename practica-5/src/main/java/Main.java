import entidades.*;
import servicios.*;
import entidades.Usuario;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.rendering.template.JavalinThymeleaf;
import servicios.BootStrapServices;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {

        // Levantar H2 como servidor
        BootStrapServices.getInstancia().init();

        // Crear usuario administrador por defecto
        Usuario admin = UsuarioServices.getInstancia().findByUsername("admin");
        if (admin == null) {
            Usuario nuevo = new Usuario("admin", "Admin", "admin", true, true, false);
            UsuarioServices.getInstancia().crear(nuevo);
        }

        // Configuracion de archivos estaticos y Thymeleaf
        var app = Javalin.create(config -> {

            config.staticFiles.add("/publico", Location.CLASSPATH);

            config.fileRenderer(new JavalinThymeleaf());
        }).start(7000);

        // Auto login por cookie
        app.before(ctx -> {

            Usuario usuarioSesion = ctx.sessionAttribute("usuario");

            if (usuarioSesion == null) {

                String cookie = ctx.cookie("recordarUsuario");

                if (cookie != null) {
                    try {
                        String username = ServiciosCripto.desencriptar(cookie);
                        Usuario usuario = UsuarioServices.getInstancia().findByUsername(username);

                        if (usuario != null) {
                            ctx.sessionAttribute("usuario", usuario);
                        }

                    } catch (Exception ignored) {
                        // cookie invalida
                    }
                }
            }
        });

        // Verificar acciones que requieren login
        app.before(ctx -> {
            String p = ctx.path();

            // acciones que requieren autenticacion
            boolean requiereLogin = p.equals("/logout") || p.contains("/comentarios");

            if (!requiereLogin) {
                return;
            }

            Usuario usuario = ctx.sessionAttribute("usuario");
            if (usuario == null) {
                ctx.redirect("/login");
            }
        });

        // Mostrar login
        app.get("/login", ctx -> {
            Usuario usuario = ctx.sessionAttribute("usuario");
            if(usuario != null){
                ctx.redirect("/");
                return;
            }
            ctx.render("templates/login.html");
        });

        // Index, pagina principal con paginacion
        app.get("/", ctx -> {

            int tam = 5;
            int pag = 1;

            String p = ctx.queryParam("page");
            if (p != null) {
                try {
                    pag = Integer.parseInt(p);
                } catch (Exception ignored) {}
            }

            if (pag < 1) {
                pag = 1;
            }

            long total = ArticuloServices.getInstancia().contarArticulos();
            int totalPages = (int) Math.ceil((double) total / tam);

            if (totalPages == 0) {
                totalPages = 1;
            }

            if (pag > totalPages) {
                pag = totalPages;
            }

            List<Articulo> articulos =
                    ArticuloServices.getInstancia().listarPaginado(pag, tam);

            List<Etiqueta> etiquetasRecientes =
                    EtiquetaServices.getInstancia().listarPrimeras(10);

            Map<String, Object> model = new HashMap<>();
            model.put("usuario", ctx.sessionAttribute("usuario"));
            model.put("articulos", articulos);
            model.put("page", pag);
            model.put("totalPages", totalPages);
            model.put("etiquetasRecientes", etiquetasRecientes);

            ctx.render("templates/index.html", model);
        });


        // Procesar login
        app.post("/procesamientoLogin", ctx -> {

            String username = ctx.formParam("usuario");
            String contrasena = ctx.formParam("contrasena");

            // validacion minima
            if (username == null || contrasena == null) {
                ctx.status(400);
                ctx.render("templates/login.html");
                return;
            }

            Usuario usuario = UsuarioServices.getInstancia().findByUsername(username);

            // verificar usuario y contraseña
            if (usuario == null || !usuario.getContrasena().equals(contrasena)) {
                Map<String, Object> modelo = new HashMap<>();
                modelo.put("error", "Usuario o contraseña incorrectos");
                ctx.status(401);
                ctx.render("templates/login.html", modelo);
                return;
            }

            // registrar en cockroach
            LoginJDBC.registrarLogin(usuario.getUsername());

            // guardar sesion
            ctx.sessionAttribute("usuario", usuario);

            // verificar si marco recordar
            String recordar = ctx.formParam("recordar");
            if (recordar != null) {
                String valorEncriptado = ServiciosCripto.encriptar(usuario.getUsername());
                ctx.cookie("recordarUsuario", valorEncriptado, 60 * 60 * 24 * 7); // 7 dias
            }

            // redirigir
            ctx.redirect("/");
        });

        // Cerrar sesion
        app.post("/logout", ctx -> {
            ctx.req().getSession().invalidate();
            ctx.removeCookie("recordarUsuario");
            ctx.redirect("/");
        });

        // ------------ CRUD USUARIO -------------- //

        // dirigir a nuevo usuario
        app.get("/usuarios/nuevo", ctx -> {
            Usuario administrador = ctx.sessionAttribute("usuario");

            if (administrador == null || !administrador.isAdministrador()) {
                ctx.status(403).result("No autorizado");
                return;
            }

            Map<String, Object> model = new HashMap<>();
            model.put("usuario", administrador);
            model.put("usuarios", UsuarioServices.getInstancia().findAll());

            ctx.render("templates/nuevoUsuario.html", model);
        });

        // crear usuario
        app.post("/usuarios", ctx -> {
            Usuario administrador = ctx.sessionAttribute("usuario");

            if (administrador == null || !administrador.isAdministrador()) {
                ctx.status(403).result("No autorizado");
                return;
            }

            String username = ctx.formParam("username");
            String nombre = ctx.formParam("nombre");
            String contrasena = ctx.formParam("contrasena");
            String rol = ctx.formParam("rol");

            if (username == null || username.isBlank() ||
                    nombre == null || nombre.isBlank() ||
                    contrasena == null || contrasena.isBlank() ||
                    rol == null || rol.isBlank()) {
                ctx.status(400).result("Faltan campos obligatorios");
                return;
            }

            // evitar duplicado
            if (UsuarioServices.getInstancia().findByUsername(username) != null) {
                ctx.status(400).result("Ese username ya existe");
                return;
            }

            boolean esAdmin = "ADMIN".equalsIgnoreCase(rol);
            boolean esAutor = "AUTOR".equalsIgnoreCase(rol);
            boolean esUsuario = "USUARIO".equalsIgnoreCase(rol);

            Usuario nuevo = new Usuario(username, nombre, contrasena, esAdmin, esAutor, esUsuario);

            var file = ctx.uploadedFile("foto");
            if (file != null) {
                byte[] bytes = file.content().readAllBytes();
                String base64 = Base64.getEncoder().encodeToString(bytes);
                nuevo.setFotoBase64(base64);
                nuevo.setFotoMimeType(file.contentType());
            }

            UsuarioServices.getInstancia().crear(nuevo);

            ctx.redirect("/usuarios/nuevo");
        });

        // eliminar usuario
        app.post("/usuarios/{username}/eliminar", ctx -> {
            Usuario administrador = ctx.sessionAttribute("usuario");

            if (administrador == null || !administrador.isAdministrador()) {
                ctx.status(403).result("No autorizado");
                return;
            }

            String username = ctx.pathParam("username");

            // evitar borrar el admin
            if (administrador.getUsername().equalsIgnoreCase(username)) {
                ctx.status(400).result("No puedes eliminar tu propio usuario.");
                return;
            }

            Usuario u = UsuarioServices.getInstancia().findByUsername(username);
            if (u == null) {
                ctx.status(404).result("Usuario no encontrado");
                return;
            }

            UsuarioServices.getInstancia().eliminar(u.getId());
            ctx.redirect("/usuarios/nuevo");
        });

        // para redirigir a editar usuario
        app.get("/usuarios/editarUsuario", ctx -> {
            Usuario administrador = ctx.sessionAttribute("usuario");

            if (administrador == null || !administrador.isAdministrador()) {
                ctx.status(403).result("No autorizado");
                return;
            }

            String username = ctx.queryParam("username");
            if (username == null || username.isBlank()) {
                ctx.status(400).result("Falta username");
                return;
            }

            Usuario usuarioEditar = UsuarioServices.getInstancia().findByUsername(username);
            if (usuarioEditar == null) {
                ctx.status(404).result("Usuario no encontrado");
                return;
            }

            Map<String, Object> model = new HashMap<>();
            model.put("usuario", administrador);
            model.put("u", usuarioEditar);

            ctx.render("templates/editarUsuario.html", model);
        });

        // actualizar usuario
        app.post("/usuarios/{username}/actualizar", ctx -> {
            Usuario administrador = ctx.sessionAttribute("usuario");

            if (administrador == null || !administrador.isAdministrador()) {
                ctx.status(403).result("No autorizado");
                return;
            }

            String username = ctx.pathParam("username");

            Usuario usuario = UsuarioServices.getInstancia().findByUsername(username);
            if (usuario == null) {
                ctx.status(404).result("Usuario no encontrado");
                return;
            }

            String nombre = ctx.formParam("nombre");
            String contrasena = ctx.formParam("contrasena");
            String rol = ctx.formParam("rol");

            if (nombre != null && !nombre.isBlank()) {
                usuario.setNombre(nombre);
            }

            if (contrasena != null && !contrasena.isBlank()) {
                usuario.setContrasena(contrasena);
            }

            boolean esAdmin = "ADMIN".equalsIgnoreCase(rol);
            boolean esAutor = "AUTOR".equalsIgnoreCase(rol);
            boolean esUsuario = "USUARIO".equalsIgnoreCase(rol);

            usuario.setAdministrador(esAdmin);
            usuario.setAutor(esAutor);
            usuario.setUsuarioNormal(esUsuario);

            var file = ctx.uploadedFile("foto");
            if (file != null) {
                byte[] bytes = file.content().readAllBytes();
                String base64 = Base64.getEncoder().encodeToString(bytes);
                usuario.setFotoBase64(base64);
                usuario.setFotoMimeType(file.contentType());
            }

            UsuarioServices.getInstancia().editar(usuario);

            ctx.redirect("/usuarios/nuevo");
        });

        // ----------- CRUD ARTICULO ----------------------

        // Dirigir a crear nuevo articulo
        app.get("/articulos/nuevoArticulo", ctx -> {
            Usuario usuario = ctx.sessionAttribute("usuario");

            // verificar que aunque se escriba la ruta en el navegador no permita entrar
            if (usuario == null || (!usuario.isAdministrador() && !usuario.isAutor())) {
                ctx.status(403).result("No autorizado");
                return;
            }

            Map<String, Object> modelo = new java.util.HashMap<>();
            modelo.put("usuario", usuario);

            ctx.render("templates/nuevoArticulo.html", modelo);
        });

        // crear articulo verificando que no se duplique
        app.post("/articulos", ctx -> {

            Usuario usuarioSesion = ctx.sessionAttribute("usuario");

            if (usuarioSesion == null || (!usuarioSesion.isAdministrador() && !usuarioSesion.isAutor())) {
                ctx.status(403).result("No autorizado");
                return;
            }

            String titulo = ctx.formParam("titulo");
            String contenido = ctx.formParam("contenido");
            String etiquetasTexto = ctx.formParam("etiquetas");

            if (titulo == null || titulo.isBlank() || contenido == null || contenido.isBlank()) {
                ctx.status(400).result("Faltan campos obligatorios");
                return;
            }

            Usuario autorBd = UsuarioServices.getInstancia().findByUsername(usuarioSesion.getUsername());

            boolean existe = ArticuloServices.getInstancia()
                    .existeArticulo(titulo, contenido, autorBd.getUsername());

            if (existe) {
                ctx.status(400).result("Este artículo ya existe.");
                return;
            }

            Articulo articulo = new Articulo(
                    titulo,
                    contenido,
                    autorBd,
                    java.time.LocalDateTime.now()
            );

            if (etiquetasTexto != null && !etiquetasTexto.isBlank()) {
                String[] partes = etiquetasTexto.split(",");

                for (String nombreEtiqueta : partes) {
                    String limpia = nombreEtiqueta.trim();
                    if (limpia.isEmpty()) continue;

                    Etiqueta et = EtiquetaServices.getInstancia().findByEtiqueta(limpia);
                    if (et == null) {
                        et = new Etiqueta(limpia);
                        EtiquetaServices.getInstancia().crear(et);
                    }

                    articulo.getListaEtiquetas().add(et);
                }
            }

            ArticuloServices.getInstancia().crear(articulo);

            ctx.redirect("/");
        });

        // ver cada detalle de articulo
        app.get("/articulos/{id}", ctx -> {

            long id;
            try {
                id = Long.parseLong(ctx.pathParam("id"));
            } catch (NumberFormatException e) {
                ctx.status(400).result("Id inválido");
                return;
            }

            Articulo articulo = ArticuloServices.getInstancia().findIdConDet(id);

            if (articulo == null) {
                ctx.status(404).result("Artículo no encontrado");
                return;
            }

            // etiquetas
            List<Articulo> ordenados = ArticuloServices.getInstancia().listarOrdenados();
            List<Articulo> recientes = ordenados.stream().limit(5).toList();

            List<Etiqueta> etiquetasRecientes = recientes.stream()
                    .flatMap(a -> a.getListaEtiquetas().stream())
                    .distinct()
                    .toList();

            Map<String, Object> model = new HashMap<>();
            model.put("usuario", ctx.sessionAttribute("usuario"));
            model.put("articulo", articulo);
            model.put("etiquetasRecientes", etiquetasRecientes);

            ctx.render("templates/articulo.html", model);
        });

        // editar articulo
        app.get("/articulos/{id}/editar", ctx -> {

            long id = Long.parseLong(ctx.pathParam("id"));

            Articulo articulo = ArticuloServices.getInstancia().findIdConDet(id);
            if (articulo == null) {
                ctx.status(404).result("Artículo no encontrado");
                return;
            }

            Usuario usuario = ctx.sessionAttribute("usuario");

            if (usuario == null ||
                    (!usuario.isAdministrador() &&
                            !usuario.getUsername().equalsIgnoreCase(articulo.getAutor().getUsername()))) {
                ctx.status(403).result("No autorizado");
                return;
            }

            String etiquetasTexto = articulo.getListaEtiquetas().stream()
                    .map(Etiqueta::getEtiqueta)
                    .collect(java.util.stream.Collectors.joining(", "));

            Map<String, Object> model = new HashMap<>();
            model.put("usuario", usuario);
            model.put("articulo", articulo);
            model.put("etiquetasTexto", etiquetasTexto);
            model.put("editando", true);

            ctx.render("templates/nuevoArticulo.html", model);
        });

        // actualizar articulo
        app.post("/articulos/{id}/actualizar", ctx -> {

            long id = Long.parseLong(ctx.pathParam("id"));

            Articulo articulo = ArticuloServices.getInstancia().findIdConDet(id);
            if (articulo == null) {
                ctx.status(404).result("No encontrado");
                return;
            }

            Usuario usuario = ctx.sessionAttribute("usuario");

            if (usuario == null ||
                    (!usuario.isAdministrador() &&
                            !usuario.getUsername().equalsIgnoreCase(articulo.getAutor().getUsername()))) {
                ctx.status(403).result("No autorizado");
                return;
            }

            String titulo = ctx.formParam("titulo");
            String contenido = ctx.formParam("contenido");
            String etiquetasTexto = ctx.formParam("etiquetas");

            if (titulo != null) articulo.setTitulo(titulo);
            if (contenido != null) articulo.setCuerpo(contenido);


            articulo.getListaEtiquetas().clear();

            if (etiquetasTexto != null && !etiquetasTexto.isBlank()) {
                String[] partes = etiquetasTexto.split(",");

                for (String parte : partes) {
                    String limpia = parte.trim();
                    if (limpia.isEmpty()) continue;

                    Etiqueta et = EtiquetaServices.getInstancia().findByEtiqueta(limpia);
                    if (et == null) {
                        et = new Etiqueta(limpia);
                        EtiquetaServices.getInstancia().crear(et);
                    }
                    articulo.getListaEtiquetas().add(et);
                }
            }

            ArticuloServices.getInstancia().editar(articulo);

            ctx.redirect("/articulos/" + id);
        });

        // eliminar articulo
        app.post("/articulos/{id}/eliminar", ctx -> {

            long id = Long.parseLong(ctx.pathParam("id"));

            Articulo articulo = ArticuloServices.getInstancia().find(id);
            if (articulo == null) {
                ctx.status(404).result("No encontrado");
                return;
            }

            Usuario usuario = ctx.sessionAttribute("usuario");

            if (usuario == null ||
                    (!usuario.isAdministrador() &&
                            !usuario.getUsername().equalsIgnoreCase(articulo.getAutor().getUsername()))) {
                ctx.status(403).result("No autorizado");
                return;
            }

            ArticuloServices.getInstancia().eliminar(id);

            ctx.redirect("/");
        });

        // ----- COMENTARIO ---------------

        // crear comentario
        app.post("/articulos/{id}/comentarios", ctx -> {

            long idArticulo = Long.parseLong(ctx.pathParam("id"));

            Usuario usuarioSesion = ctx.sessionAttribute("usuario");
            if (usuarioSesion == null) {
                ctx.redirect("/login");
                return;
            }

            String texto = ctx.formParam("comentario");
            if (texto == null || texto.isBlank()) {
                ctx.redirect("/articulos/" + idArticulo);
                return;
            }

            Articulo articulo = ArticuloServices.getInstancia().find(idArticulo);
            if (articulo == null) {
                ctx.status(404).result("Artículo no encontrado");
                return;
            }

            Usuario autorBd = UsuarioServices.getInstancia()
                    .findByUsername(usuarioSesion.getUsername());

            Comentario c = new Comentario(
                    texto,
                    autorBd,
                    articulo,
                    java.time.LocalDateTime.now()
            );

            ComentarioServices.getInstancia().crear(c);

            ctx.redirect("/articulos/" + idArticulo);
        });

        // eliminar comentario
        app.post("/articulos/{id}/comentarios/{idComentario}/eliminar", ctx -> {

            long idArticulo = Long.parseLong(ctx.pathParam("id"));
            long idComentario = Long.parseLong(ctx.pathParam("idComentario"));

            Usuario usuarioSesion = ctx.sessionAttribute("usuario");
            if (usuarioSesion == null) {
                ctx.redirect("/login");
                return;
            }

            Comentario comentario = ComentarioServices.getInstancia().find(idComentario);
            if (comentario == null) {
                ctx.status(404).result("Comentario no encontrado");
                return;
            }

            boolean esAdmin = usuarioSesion.isAdministrador();
            boolean esAutorComentario =
                    comentario.getAutor() != null &&
                            usuarioSesion.getUsername().equalsIgnoreCase(comentario.getAutor().getUsername());

            if (!esAdmin && !esAutorComentario) {
                ctx.status(403).result("No autorizado");
                return;
            }

            ComentarioServices.getInstancia().eliminar(idComentario);

            ctx.redirect("/articulos/" + idArticulo);
        });

        // --------- ARTICULOS RELACIONADOS A ESA ETIQUETA -----------------------
        app.get("/etiquetas/{nombre}", ctx -> {

            String nombre = ctx.pathParam("nombre");

            List<Articulo> articulos = ArticuloServices.getInstancia().listarPorEtiqueta(nombre);

            List<Articulo> recientesParaSidebar = ArticuloServices.getInstancia()
                    .listarOrdenados()
                    .stream().limit(5).toList();

            List<Etiqueta> etiquetasRecientes = recientesParaSidebar.stream()
                    .flatMap(a -> a.getListaEtiquetas().stream())
                    .distinct()
                    .toList();

            Map<String, Object> model = new HashMap<>();
            model.put("usuario", ctx.sessionAttribute("usuario"));
            model.put("articulos", articulos);
            model.put("etiquetasRecientes", etiquetasRecientes);

            model.put("filtroEtiqueta", nombre);

            model.put("page", 1);
            model.put("totalPages", 1);

            ctx.render("templates/index.html", model);
        });

    }
}
