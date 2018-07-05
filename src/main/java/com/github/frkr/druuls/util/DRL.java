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

package com.github.frkr.druuls.util;

import com.github.frkr.druuls.banco.Rule;
import com.github.frkr.druuls.dao.Entrada;
import com.github.frkr.druuls.dao.Saida;
import com.github.frkr.druuls.rest.DroolsRest;
import org.kie.api.runtime.KieSession;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class DRL {

    public static final Set<String> constTipos = new HashSet<>();

    static {
        constTipos.add("String");
        constTipos.add("Integer");
        constTipos.add("Long");
        constTipos.add("Double");
        constTipos.add("Boolean");
    }

    private String drl = "";
    private String saida = "";
    private String erros = "";
    private Map<String, Map<String, String>> listaDominio = new TreeMap<>();
    private Set<String> listaTipo = new HashSet<>();
    private Map<String, Map<String, String>> listaFato = new TreeMap<>();
    private Map<String, Map<String, String>> listaResultado = new TreeMap<>();
    private Long idGerado = null;
    private String titulo = "";

    public DRL() {
    }

    //region Leitura
    public DRL(Rule rule) {
        this.setIdGerado(rule.getId());
        this.setTitulo(rule.getTitulo());

        this.setSaida(rule.getDrl().split("v : ")[1].split("\\(\\)")[0]);

        String[] declares = rule.getDrl().split("declare ");
        Map<String, String> declarando;

        for (int i = 1; i < declares.length; i++) {
            declarando = new TreeMap<>();
            String[] tipo = declares[i].split("\t");
            for (int i2 = 1; i2 < declares.length; i2++) {
                declarando.put(tipo[i2].split(":")[0].trim(), tipo[i2].split(":")[1].split("end")[0].trim());
            }
            listaTipo.add(declares[i].split("\n")[0]);
            listaDominio.put(declares[i].split("\n")[0], declarando);
        }

        String[] fatos = rule.getDrl().split("rule \"Regra: ");
        for (int i = 1; i < fatos.length; i++) {
            String fatoUsuario = fatos[i].split("\"")[0];
            String[] criticas = fatos[i].split("then")[0].split("\t\t");
            String[] resultados = fatos[i].split("then")[1].split("\t\t");
            Map<String, String> critica = new HashMap<>();
            for (int j = 1; j < criticas.length; j++) {
                AtomicReference<String> tipo = new AtomicReference<>(criticas[j].split(":")[0].trim());
                listaTipo.stream().forEach( s -> {
                    if (s.equalsIgnoreCase(tipo.get())) {
                        tipo.set(s);
                    }
                });
                critica.put(tipo.get(), criticas[j].split(":")[1].split(tipo.get()+"\\(")[1].split("\\)")[0]);
            }
            this.listaFato.put(fatoUsuario, critica);
            StringBuilder ultimaLinha = new StringBuilder();
            for (int j = 1; j < resultados.length - 1; j++) {
                ultimaLinha.append(resultados[j]);
            }
            AtomicReference<String> saida = new AtomicReference<>(fatos[i].split("update\\(")[1].split("\\)")[0]);
            listaTipo.stream().forEach( s -> {
                if (s.equalsIgnoreCase(saida.get())) {
                    saida.set(s);
                }
            });
            Map<String, String> resultado = new HashMap<>();
            resultado.put(saida.get(), ultimaLinha.toString());
            this.listaResultado.put(fatoUsuario, resultado);
        }

        build();
    }
    //endregion

    //region MAIN TEST TODO Deletar main
    public static void main(String[] args) {
        Rule rule = new Rule();
        rule.setId(1l);
        rule.setTitulo("teste");
        rule.setDrl("import com.github.frkr.druuls.dao.Entrada\n" +
                "import com.github.frkr.druuls.dao.Saida\n" +
                "\n" +
                "declare Aprovacao\n" +
                "\tAprovado : Boolean\n" +
                "\tCenario : Integer\n" +
                "end\n" +
                "\n" +
                "declare Perfil\n" +
                "\tCenario : Integer\n" +
                "\tNome : String\n" +
                "end\n" +
                "\n" +
                "rule \"Entrada\"\n" +
                "no-loop\n" +
                "\twhen\n" +
                "\t\tentrada : Entrada()\n" +
                "\tthen\n" +
                "\t\tAprovacao aprovacao = new Aprovacao();\n" +
                "\t\taprovacao.setAprovado(null);\n" +
                "\t\taprovacao.setCenario(null);\n" +
                "\t\tinsert(aprovacao);\n" +
                "\n" +
                "\t\tPerfil perfil = new Perfil();\n" +
                "\t\tperfil.setCenario(Integer.parseInt(entrada.getValues().get(\"Cenario\")));\n" +
                "\t\tperfil.setNome(entrada.getValues().get(\"Nome\"));\n" +
                "\t\tinsert(perfil);\n" +
                "\n" +
                "end\n" +
                "\n" +
                "rule \"Saida\"\n" +
                "no-loop\n" +
                "\twhen\n" +
                "\t\tsaida : Saida()\n" +
                "\t\tv : Aprovacao()\n" +
                "\tthen\n" +
                "\t\tsaida.getValues().put(\"Aprovado\", v.getAprovado()+\"\");\n" +
                "\t\tsaida.getValues().put(\"Cenario\", v.getCenario()+\"\");\n" +
                "\t\tupdate(saida);\n" +
                "end\n" +
                "\n" +
                "rule \"Regra: Regra do Usuário\"\n" +
                "no-loop\n" +
                "\twhen\n" +
                "\t\tperfil : Perfil(Cenario > 500)\n" +
                "\t\taprovacao : Aprovacao()\n" +
                "\tthen\n" +
                "\t\taprovacao.setCenario(perfil.getCenario());\n" +
                "\t\taprovacao.setAprovado(true);\n" +
                "\t\tupdate(aprovacao);\n" +
                "end\n" +
                "\n");

        DRL drl = new DRL(rule);
        System.out.println(drl.getErros());
        System.out.println();
        System.out.println();
        drl.build();
        System.out.println(drl.getDrl());
        System.out.println();
        System.out.println();
        System.out.println(rule.getDrl());
    }
    //endregion

    //region Gerar
    public void build() {
        StringBuilder rule = new StringBuilder();

        rule.append("import ");
        rule.append(Entrada.class.getCanonicalName());
        rule.append("\n");
        rule.append("import ");
        rule.append(Saida.class.getCanonicalName());
        rule.append("\n\n");

        for (Map.Entry<String, Map<String, String>> tipo : listaDominio.entrySet()) {
            rule.append("declare ");
            rule.append(tipo.getKey());
            rule.append("\n");
            for (Map.Entry<String, String> variavel : tipo.getValue().entrySet()) {
                rule.append("\t");
                rule.append(variavel.getKey());
                rule.append(" : ");
                rule.append(variavel.getValue());
                rule.append("\n");
            }
            rule.append("end\n\n");
        }

        rule.append("rule \"Entrada\"\n");
        rule.append("no-loop\n");
        rule.append("\twhen\n");
        rule.append("\t\tentrada : Entrada()\n");
        rule.append("\tthen\n");

        for (Map.Entry<String, Map<String, String>> tipo : listaDominio.entrySet()) {
            rule.append("\t\t");
            rule.append(tipo.getKey());
            rule.append(" ");
            rule.append(tipo.getKey().toLowerCase());
            rule.append(" = new ");
            rule.append(tipo.getKey());
            rule.append("();\n");
            for (Map.Entry<String, String> variavel : tipo.getValue().entrySet()) {
                rule.append("\t\t");
                rule.append(tipo.getKey().toLowerCase());
                rule.append(".set");
                rule.append(variavel.getKey());
                rule.append("(");

                if (!saida.equals(tipo.getKey())) {
                    rule.append(parse1(variavel.getValue()));
                    rule.append("entrada.getValues().get(\"");
                    rule.append(variavel.getKey());
                    rule.append("\")");
                    rule.append(parse2(variavel.getValue()));
                } else {
                    rule.append("null");
                }

                rule.append(");");
                rule.append("\n");
            }
            rule.append("\t\tinsert(");
            rule.append(tipo.getKey().toLowerCase());
            rule.append(");\n\n");
        }
        rule.append("end\n\n");

        rule.append("rule \"Saida\"\n");
        rule.append("no-loop\n");
        rule.append("\twhen\n");
        rule.append("\t\tsaida : Saida()\n");
        rule.append("\t\tv : ");
        rule.append(saida);
        rule.append("()\n");
        rule.append("\tthen\n");

        for (String variavel : listaDominio.get(saida).keySet()) {
            rule.append("\t\tsaida.getValues().put(\"");
            rule.append(variavel);
            rule.append("\", v.get");
            rule.append(variavel);
            rule.append("()+\"\");\n");
        }
        rule.append("\t\tupdate(saida);\n");
        rule.append("end\n\n");

        for (Map.Entry<String, Map<String, String>> criticas : listaFato.entrySet()) {
            rule.append("rule \"Regra: ");
            rule.append(criticas.getKey());
            rule.append("\"\nno-loop\n\twhen\n");
            for (Map.Entry<String, String> critica : criticas.getValue().entrySet()) {
                rule.append("\t\t");
                rule.append(critica.getKey().toLowerCase());
                rule.append(" : ");
                rule.append(critica.getKey());
                rule.append("(");
                rule.append(critica.getValue());
                rule.append(")");
                rule.append("\n");
            }
            rule.append("\tthen\n");
            for (Map.Entry<String, String> resultado : listaResultado.get(criticas.getKey()).entrySet()) {
                rule.append("\t\t");
                rule.append(resultado.getValue().replaceAll("\n", "\n\t\t"));
                rule.append("\n");
                rule.append("\t\tupdate(");
                rule.append(resultado.getKey().toLowerCase());
                rule.append(");\n");
            }
            rule.append("end\n");
        }

        rule.append("\n");
        KieSession ks = null;
        try {
            ks = DroolsRest.initDrools(rule.toString());
        } catch (Exception e) {
            erros = e.getMessage();
        } finally {
            if (ks != null) {
                ks.dispose();
            }
        }
        this.drl = rule.toString();
    }

    public static String parse1(String tipo) {
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

    public static String parse2(String tipo) {
        if ("String".equals(tipo)) {
            return "";
        }
        return ")";
    }
    //endregion

    //region GETSET
    public String getDrl() {
        return drl;
    }

    public void setDrl(String drl) {
        this.drl = drl;
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
        this.erros = erros;
    }

    public Map<String, Map<String, String>> getListaDominio() {
        return listaDominio;
    }

    public void setListaDominio(Map<String, Map<String, String>> listaDominio) {
        this.listaDominio = listaDominio;
    }

    public Set<String> getListaTipo() {
        return listaTipo;
    }

    public void setListaTipo(Set<String> listaTipo) {
        this.listaTipo = listaTipo;
    }

    public Map<String, Map<String, String>> getListaFato() {
        return listaFato;
    }

    public void setListaFato(Map<String, Map<String, String>> listaFato) {
        this.listaFato = listaFato;
    }

    public Map<String, Map<String, String>> getListaResultado() {
        return listaResultado;
    }

    public void setListaResultado(Map<String, Map<String, String>> listaResultado) {
        this.listaResultado = listaResultado;
    }

    public Long getIdGerado() {
        return idGerado;
    }

    public void setIdGerado(Long idGerado) {
        this.idGerado = idGerado;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }
    //endregion
}
