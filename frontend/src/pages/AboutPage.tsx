import { Link } from "react-router-dom";

export default function AboutPage() {
  return (
    <div className="page">
      <div className="topbar">
        <h1>Sobre</h1>
        <Link to="/">Voltar</Link>
      </div>

      <h2>Como é composta uma receita</h2>
      <p>
        Cada receita tem um nome, uma lista de ingredientes, o modo de preparo, os utensílios
        necessários e um espaço para dicas. O nome só precisa de ser único entre as suas receitas —
        outra pessoa pode ter uma receita com o mesmo nome.
      </p>
      <p>
        Cada ingrediente é composto por quantidade, tipo de quantidade e item. Por exemplo:
        1 / colher / açúcar. Os três campos são texto livre, para não o obrigar a encaixar
        as coisas em listas fechadas.
      </p>
      <p>
        O modo de preparo é dividido em partes — massa, recheio, molho — e cada parte tem os
        seus passos pela ordem em que se fazem. Cada passo pode ter um tempo em minutos, e a
        aplicação soma os tempos de cada parte. Se algum passo não tiver tempo, o total aparece
        como aproximado.
      </p>

      <h2>Criar e editar</h2>
      <p>
        Qualquer receita que crie fica automaticamente em seu nome. Só você a pode editar ou
        apagar, mas toda a gente a pode ler e cozinhar.
      </p>

      <h2>O que é uma receita ativa</h2>
      <p>
        Ativar uma receita é dizer "vou fazer esta". Pode ter até cinco ativas ao mesmo tempo.
        Com a receita ativa, cada ingrediente ganha um interruptor para marcar se o tem em casa
        ou não.
      </p>
      <p>
        Tudo o que ficar marcado como em falta aparece automaticamente na lista de compras. Quando
        risca um item na lista, ele passa a contar como "tenho" na receita. E se desativar a
        receita, os itens dela desaparecem da lista — sem ter de os limpar à mão.
      </p>
    </div>
  );
}
