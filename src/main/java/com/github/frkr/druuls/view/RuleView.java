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
import com.github.frkr.druuls.dao.RuleRepository;
import com.github.frkr.druuls.util.DRL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.util.*;

@Component
@SessionScope
public class RuleView {

    private DRL atual = new DRL();

    @Autowired
    private RuleRepository dao;

    // TODO Apagar init
    @PostConstruct
    public void init() {
        DRL drl = new DRL();
        drl.setTitulo("Regra 01");

        Map<String, String> declarando = new TreeMap<>();
        declarando.put("Nome", "String");
        declarando.put("Cenario", "Integer");
        drl.getListaTipo().add("Perfil");
        drl.getListaDominio().put("Perfil", declarando);

        declarando = new TreeMap<>();
        declarando.put("Cenario", "Integer");
        declarando.put("Aprovado", "Boolean");
        drl.getListaTipo().add("Aprovacao");
        drl.getListaDominio().put("Aprovacao", declarando);
        drl.setSaida("Aprovacao");

        String fatoUsuario = "Aprovar Cenário";
        Map<String, String> critica = new HashMap<>();
        critica.put("Perfil", "Cenario > 500");
        critica.put("Aprovacao", "");
        drl.getListaFato().put(fatoUsuario, critica);
        Map<String, String> resultado = new HashMap<>();
        resultado.put("Aprovacao", "aprovacao.setCenario(perfil.getCenario());\naprovacao.setAprovado(true);");
        drl.getListaResultado().put(fatoUsuario, resultado);

        drl.build();
        Rule rule = new Rule();
        rule.setDrl(drl.getDrl());
        rule.setTitulo(drl.getTitulo());
        dao.saveAndFlush(rule);
    }

    public void carregar(Long id) {
        atual = new DRL();
        try {
            Rule rule = dao.findById(id).get();
            atual = new DRL(rule);
            if (!"".equals(atual.getErros())) {
                throw new Exception(atual.getErros());
            }
        } catch (Exception e) {
            e.printStackTrace(); // TODO logger
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Drools", e.getMessage()));
        }
    }

    public List<Rule> tudo() {
        return dao.findAll();
    }

    public Set<String> listaTipo() {
        return DRL.constTipos;
    }

    //region GETSET
    public DRL getAtual() {
        return atual;
    }

    public void setAtual(DRL atual) {
        if (atual == null) {
            this.atual = new DRL();
        } else {
            this.atual = atual;
        }
    }
    //endregion
}
