package es.caib.concsv.api.interna;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

@WebServlet("/swagger/*")
public class SwaggerStaticServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {

		String path = req.getPathInfo(); // ej: /index.html
		if (path == null || path.equals("/")) {
			path = "/index.html";
		}

		try (InputStream in = getClass().getResourceAsStream("/swagger" + path)) {
			if (in == null) {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			// podrías poner content-type según extensión aquí
			resp.setContentType(getServletContext().getMimeType(path));
			in.transferTo(resp.getOutputStream());
		}
	}
}
