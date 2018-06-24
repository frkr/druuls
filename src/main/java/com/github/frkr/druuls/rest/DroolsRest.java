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

package com.github.frkr.druuls.rest;

import com.github.frkr.druuls.dao.Entrada;
import com.github.frkr.druuls.dao.Saida;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api")
public class DroolsRest {

    @RequestMapping(value = "execute", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Saida execute(@RequestBody Entrada entrada) throws Exception {
        KieSession ks = null;
        try {
            ks = initDrools();
            ks.insert(entrada);

            Saida saida = new Saida();
            FactHandle factSaida = ks.insert(saida);

            ks.fireAllRules();
            return (Saida) ks.getObject(factSaida);
        } finally {
            if (ks != null) {
                ks.dispose();
            }
        }
    }

    private static KieSession initDrools() throws Exception {
        String myRule = "import com.github.frkr.druuls.dao.Entrada\n" +
                "import com.github.frkr.druuls.dao.Saida\n" +
                "\n" +
                "// DECLARAR TODOS OS TIPOS\n" +
                "declare  Perfil\n" +
                "    Nome : String\n" +
                "    Cenario : Integer\n" +
                "end\n" +
                "\n" +
                "declare Aprovacao\n" +
                "    Cenario : Integer\n" +
                "    Aprovado : Boolean\n" +
                "end\n" +
                "\n" +
                "rule \"Entrada\"\n" +
                "no-loop\n" +
                "    when\n" +
                "        entrada : Entrada()\n" +
                "    then\n" +
                "        // PEGAR TODOS OS VALORES DA ENTRADA\n" +
                "        // INICIALIZAR TODOS OS TIPOS\n" +
                "        Perfil perfil = new Perfil();\n" +
                "        perfil.setNome(entrada.getValues().get(\"Nome\"));\n" +
                "        perfil.setCenario( Integer.parseInt( entrada.getValues().get(\"Cenario\") ) );\n" +
                "        insert(perfil);\n" +
                "        Aprovacao aprovacao = new Aprovacao();\n" +
                "        aprovacao.setCenario(null);\n" +
                "        aprovacao.setAprovado(false);\n" +
                "        insert(aprovacao);\n" +
                "end\n" +
                "\n" +
                "rule \"Saida\"\n" +
                "no-loop\n" +
                "    when\n" +
                "        saida : Saida()\n" +
                "        v : Aprovacao()\n" +
                "    then\n" +
                "        // ESCOLHER UM TIPO PARA SER A SAIDA\n" +
                "        saida.getValues().put(\"Cenario\", v.getCenario()+\"\");\n" +
                "        saida.getValues().put(\"Aprovado\", v.getAprovado()+\"\");\n" +
                "        update(saida);\n" +
                "end\n" +
                "\n" +
                "// COLOCAR UMA OU MAIS REGRAS DE USUARIO\n" +
                "\n" +
                "rule \"Regra do Usuario\"\n" +
                "no-loop\n" +
                "    when\n" +
                "        perfil : Perfil( Cenario > 500 )\n" +
                "        aprovacao : Aprovacao()\n" +
                "    then\n" +
                "        aprovacao.setCenario(perfil.getCenario());\n" +
                "        aprovacao.setAprovado(true);\n" +
                "        update(aprovacao);\n" +
                "end\n";

        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kfs = kieServices.newKieFileSystem();
        kfs.write("src/main/resources/rule.drl", kieServices.getResources().newByteArrayResource(myRule.getBytes()));
        KieBuilder kieBuilder = kieServices.newKieBuilder(kfs).buildAll();

        Results results = kieBuilder.getResults();
        if (results.hasMessages(Message.Level.ERROR)) {
            throw new Exception(results.getMessages().toString());
        }
        KieContainer kieContainer = kieServices.newKieContainer(kieBuilder.getKieModule().getReleaseId());
        return kieContainer.newKieSession();
    }
}
