package net.aooms.core.web.render;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * html渲染
 * Created by cccyb on 2018-04-20
 */
public class HtmlRender extends AbstractRender {

    public HtmlRender() {
        this.renderType = RenderType.HTML;
    }

    @Override
    public void render(HttpServletResponse response, Object value) throws Exception {
        response.setContentType(renderType.getContentType());
        //response.getWriter().write(String.valueOf(value));

        this.springMvcRender(value);
        //this.flushAndClose(response);
    }
}