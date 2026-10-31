package com.proyecto.servicios.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.model.gestopago.SendTxRespuesta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SendTxResponseParser {

    private static final List<String> CAMPOS_CODIGO = List.of("codigo", "code");
    private static final List<String> CAMPOS_MENSAJE = List.of("texto", "mensaje", "message");
    private static final List<String> CAMPOS_AUTORIZACION = List.of("num_autorizacion");
    private static final List<String> CAMPOS_ID_TX = List.of("id_tx");
    private static final List<String> CAMPOS_CANJE = List.of("pin");
    private static final List<String> CAMPOS_LEYENDA = List.of("legend");

    private static final Set<String> VALORES_VACIOS = Set.of("-1", "0");

    private final ObjectMapper objectMapper;

    public SendTxRespuesta parse(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            throw new IllegalArgumentException("Respuesta vacía de GestoPago");
        }
        String trimmed = cuerpo.trim();
        Map<String, String> campos = trimmed.startsWith("<") ? leerXml(trimmed) : leerJson(trimmed);
        return new SendTxRespuesta(
                normalizarCodigo(buscar(campos, CAMPOS_CODIGO)),
                buscar(campos, CAMPOS_MENSAJE),
                sinValorVacio(buscar(campos, CAMPOS_AUTORIZACION)),
                sinValorVacio(buscar(campos, CAMPOS_ID_TX)),
                buscar(campos, CAMPOS_CANJE),
                buscar(campos, CAMPOS_LEYENDA),
                cuerpo);
    }

    private Map<String, String> leerXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));

            Map<String, String> campos = new HashMap<>();
            NodeList nodos = document.getElementsByTagName("*");
            for (int i = 0; i < nodos.getLength(); i++) {
                Element elemento = (Element) nodos.item(i);
                if (!tieneHijosElemento(elemento)) {
                    campos.putIfAbsent(elemento.getTagName().toLowerCase(Locale.ROOT), elemento.getTextContent().trim());
                }
            }
            return campos;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Respuesta XML de GestoPago no válida", ex);
        }
    }

    private Map<String, String> leerJson(String json) {
        try {
            Map<String, String> campos = new HashMap<>();
            aplanar(objectMapper.readTree(json), campos);
            return campos;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Respuesta JSON de GestoPago no válida", ex);
        }
    }

    private static void aplanar(JsonNode nodo, Map<String, String> campos) {
        if (nodo == null || !nodo.isObject()) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> it = nodo.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entrada = it.next();
            if (entrada.getValue().isValueNode()) {
                campos.putIfAbsent(entrada.getKey().toLowerCase(Locale.ROOT), entrada.getValue().asText());
            } else {
                aplanar(entrada.getValue(), campos);
            }
        }
    }

    private static boolean tieneHijosElemento(Element elemento) {
        NodeList hijos = elemento.getChildNodes();
        for (int i = 0; i < hijos.getLength(); i++) {
            if (hijos.item(i).getNodeType() == Node.ELEMENT_NODE) {
                return true;
            }
        }
        return false;
    }

    private static String buscar(Map<String, String> campos, List<String> alias) {
        return alias.stream()
                .map(campos::get)
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElse(null);
    }

    private static String sinValorVacio(String valor) {
        return valor == null || VALORES_VACIOS.contains(valor) ? null : valor;
    }

    private static String normalizarCodigo(String codigo) {
        if (codigo != null && codigo.length() == 1 && Character.isDigit(codigo.charAt(0))) {
            return "0" + codigo;
        }
        return codigo;
    }
}
