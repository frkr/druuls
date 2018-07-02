/*
 * MIT License
 *
 * Copyright (c) 2018 Davi Saranszky Mesquita https://github.com/frkr/druuls
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.frkr.druuls.view;

import com.github.frkr.druuls.banco.Rule;
import com.github.frkr.druuls.dao.Entrada;
import com.github.frkr.druuls.dao.RuleRepository;
import com.github.frkr.druuls.dao.Saida;
import com.github.frkr.druuls.rest.DroolsRest;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import javax.annotation.PostConstruct;
import java.util.*;

@Component
@SessionScope
public class RuleView {

    private Set<String> constTipos = new HashSet<>();
    private String saida = "";
    private String erros = "";
    private Map<String, Map<String, String>> variaveis = new TreeMap<>();
    private Set<String> tipos = new HashSet<>();
    private Map<String, Map<String, String>> fatos = new TreeMap<>();
    private Map<String, Map<String, String>> resultados = new TreeMap<>();
    private Long idGerado = 0l;
    private String titulo = "Regra 01";

    @Autowired
    private RuleRepository dao;

    @PostConstruct
    public void init() {
        constTipos.add("String");
        constTipos.add("Integer");
        constTipos.add("Long");
        constTipos.add("Double");
        constTipos.add("Boolean");

        Map<String, String> declarando = new TreeMap<>();
        declarando.put("Nome", "String");
        declarando.put("Cenario", "Integer");
        tipos.add("Perfil");
        variaveis.put("Perfil", declarando);

        declarando = new TreeMap<>();
        declarando.put("Cenario", "Integer");
        declarando.put("Aprovado", "Boolean");
        tipos.add("Aprovacao");
        variaveis.put("Aprovacao", declarando);
        saida = "Aprovacao";

        String fatoUsuario = "Regra do Usuário";
        Map<String, String> critica = new HashMap<>();
        critica.put("Perfil", "Cenario > 500");
        critica.put("Aprovacao", "");
        fatos.put(fatoUsuario, critica);
        Map<String, String> resultado = new HashMap<>();
        resultado.put("Aprovacao", "aprovacao.setCenario(perfil.getCenario());\naprovacao.setAprovado(true);");
        resultados.put(fatoUsuario, resultado);

        Rule rule = new Rule();
        rule.setDrl(construirDRL(new StringBuilder(), tipos, variaveis, saida, fatos, resultados));
        rule.setTitulo(titulo);
        rule = dao.saveAndFlush(rule);
        idGerado = rule.getId();
    }

    public List<Rule> tudo() {
        return dao.findAll();
    }

    //region DRL
    public static String construirDRL(StringBuilder erros, Set<String> tipos, Map<String, Map<String, String>> variaveis, String saida, Map<String, Map<String, String>> fatos, Map<String, Map<String, String>> resultados) {
        StringBuilder rt = new StringBuilder();

        rt.append("import ");
        rt.append(Entrada.class.getCanonicalName());
        rt.append("\n");
        rt.append("import ");
        rt.append(Saida.class.getCanonicalName());
        rt.append("\n\n");

        for (Map.Entry<String, Map<String, String>> tipo : variaveis.entrySet()) {
            rt.append("declare ");
            rt.append(tipo.getKey());
            rt.append("\n");
            for (Map.Entry<String, String> variavel : tipo.getValue().entrySet()) {
                rt.append("\t");
                rt.append(variavel.getKey());
                rt.append(" : ");
                rt.append(variavel.getValue());
                rt.append("\n");
            }
            rt.append("end\n\n");
        }

        rt.append("rule \"Entrada\"\n");
        rt.append("no-loop\n");
        rt.append("\twhen\n");
        rt.append("\t\tentrada : Entrada()\n");
        rt.append("\tthen\n");

        for (Map.Entry<String, Map<String, String>> tipo : variaveis.entrySet()) {
            rt.append("\t\t");
            rt.append(tipo.getKey());
            rt.append(" ");
            rt.append(tipo.getKey().toLowerCase());
            rt.append(" = new ");
            rt.append(tipo.getKey());
            rt.append("();\n");
            for (Map.Entry<String, String> variavel : tipo.getValue().entrySet()) {
                rt.append("\t\t");
                rt.append(tipo.getKey().toLowerCase());
                rt.append(".set");
                rt.append(variavel.getKey());
                rt.append("(");

                if (!saida.equals(tipo.getKey())) {
                    rt.append(codigoTipo1(variavel.getValue()));
                    rt.append("entrada.getValues().get(\"");
                    rt.append(variavel.getKey());
                    rt.append("\")");
                    rt.append(codigoTipo2(variavel.getValue()));
                } else {
                    rt.append("null");
                }

                rt.append(");");
                rt.append("\n");
            }
            rt.append("\t\tinsert(");
            rt.append(tipo.getKey().toLowerCase());
            rt.append(");\n\n");
        }
        rt.append("end\n\n");

        rt.append("rule \"Saida\"\n");
        rt.append("no-loop\n");
        rt.append("\twhen\n");
        rt.append("\t\tsaida : Saida()\n");
        rt.append("\t\tv : ");
        rt.append(saida);
        rt.append("()\n");
        rt.append("\tthen\n");

        for (String variavel : variaveis.get(saida).keySet()) {
            rt.append("\t\tsaida.getValues().put(\"");
            rt.append(variavel);
            rt.append("\", v.get");
            rt.append(variavel);
            rt.append("()+\"\");\n");
        }
        rt.append("\t\tupdate(saida);\n");
        rt.append("end\n\n");

        for (Map.Entry<String, Map<String, String>> criticas : fatos.entrySet()) {
            rt.append("rule \"");
            rt.append(criticas.getKey());
            rt.append("\"\nno-loop\n\twhen\n");
            for (Map.Entry<String, String> critica : criticas.getValue().entrySet()) {
                rt.append("\t\t");
                rt.append(critica.getKey().toLowerCase());
                rt.append(" : ");
                rt.append(critica.getKey());
                rt.append("(");
                rt.append(critica.getValue());
                rt.append(")");
                rt.append("\n");
            }
            rt.append("\tthen\n");
            for (Map.Entry<String, String> resultado : resultados.get(criticas.getKey()).entrySet()) {
                rt.append("\t\t");
                rt.append(resultado.getValue().replaceAll("\n", "\n\t\t"));
                rt.append("\n");
                rt.append("\t\tupdate(");
                rt.append(resultado.getKey().toLowerCase());
                rt.append(");\n");
            }
            rt.append("end\n");
        }

        rt.append("\n");
        KieSession ks = null;
        try {
            ks = DroolsRest.initDrools(rt.toString());
        } catch (Exception e) {
            erros.append(e.getMessage());
        } finally {
            if (ks != null) {
                ks.dispose();
            }
        }
        return rt.toString();
    }

    public static String codigoTipo1(String tipo) {
        switch (tipo) {
            case "Integer":
                return "Integer.parseInt(";
            case "Long":
                return "Long.parseLong(";
            case "Double":
                return "Double.parseDouble(";
            case "Boolean":
                return "Boolean.parseBoolean(";
        }
        return "";
    }

    public static String codigoTipo2(String tipo) {
        if ("String".equals(tipo)) {
            return "";
        }
        return ")";
    }
    //endregion

    //region GETSET
    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public Map<String, Map<String, String>> getFatos() {
        return fatos;
    }

    public void setFatos(Map<String, Map<String, String>> fatos) {
        this.fatos = fatos;
    }

    public Map<String, Map<String, String>> getResultados() {
        return resultados;
    }

    public void setResultados(Map<String, Map<String, String>> resultados) {
        this.resultados = resultados;
    }

    public Long getIdGerado() {
        return idGerado;
    }

    public void setIdGerado(Long idGerado) {
        this.idGerado = idGerado;
    }

    public Set<String> getConstTipos() {
        return constTipos;
    }

    public void setConstTipos(Set<String> constTipos) {
        this.constTipos = constTipos;
    }

    public Map<String, Map<String, String>> getVariaveis() {
        return variaveis;
    }

    public void setVariaveis(Map<String, Map<String, String>> variaveis) {
        this.variaveis = variaveis;
    }

    public Set<String> getTipos() {
        return tipos;
    }

    public void setTipos(Set<String> tipos) {
        this.tipos = tipos;
    }

    public String getSaida() {
        return saida;
    }

    public void setSaida(String saida) {
        this.saida = saida;
    }

    public String getErros() {
        return erros;
    }

    public void setErros(String erros) {
    }

    public void setRule(String rule) {
    }

    public String getRule() {
        StringBuilder err = new StringBuilder();
        String rt = construirDRL(err, tipos, variaveis, saida, fatos, resultados);
        erros = err.toString();
        return rt;
    }
//endregion
}
