package com.nexatech.nexacare.config;

import com.nexatech.nexacare.security.AuthInterceptor;
import com.nexatech.nexacare.security.UsuarioLogado;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Permite declarar "UsuarioLogado usuario" como parâmetro de qualquer método de controller. */
@Component
public class UsuarioLogadoResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter p) {
        return UsuarioLogado.class.equals(p.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter p, ModelAndViewContainer m, NativeWebRequest req,
                                  WebDataBinderFactory f) {
        return req.getAttribute(AuthInterceptor.ATRIBUTO_USUARIO, RequestAttributes.SCOPE_REQUEST);
    }
}
