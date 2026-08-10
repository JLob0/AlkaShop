# AlkaShop

Sistema universal de vendas para a rede Alka* (Paper 1.21.8 / Java 21). Preço é
global por `Material` — nunca por mina, nunca por plugin — e é o único plugin da
rede que sabe "quanto vale" um item.

## O que faz

- **Preços globais por material**, configurados em `prices.yml`, com suporte a
  múltiplas moedas simultâneas via AlkaEconomy (ex: um item pode valer coins **e**
  drakonio ao mesmo tempo).
- **Cálculo automático de proporção via receita de crafting**: itens sem preço
  definido podem herdar valor proporcional aos seus ingredientes (ex: um bloco de
  ferro vale ~9x o preço do lingote, sem precisar cadastrar preço manualmente).
- **GUI principal de vendas** (`/vender`) e **baú virtual de venda selecionada**
  (`/vendersel`) — dropa item(s) ali dentro pra vender sem abrir o inventário todo.
- **Venda automática por material/categoria (auto-sell)**, restrita a jogadores
  com a permissão `alkashop.autosell` (pensado pra VIPs) — não é mais um único
  toggle global: o jogador liga "tudo" (`/venderautomatico todos`, requer
  `alkashop.autosell.all`) ou escolhe material a material
  (`/venderautomatico <material>`, permissão de categoria opcional
  `alkashop.autosell.<categoria>` se o material tiver uma em `prices.yml`) via
  `/venderautomatico` (abre `AutoSellConfigMenu`, com filtro por categoria).
- **Vender tudo / por categoria / item na mão** (`/vendertudo [categoria]`,
  `/vendermao`) pra fluxo rápido sem abrir GUI.
- **API pública** (`AlkaShopAPI`) publicada via `ServicesManager`, consumida por
  outros plugins Alka* (ex: AlkaMines, AlkaDrop) via soft-dependency —
  `isAutoSellActive(player, material)` (material-aware, respeita a escolha por
  categoria) e `sellItem(...)` permitem que um bloco minerado seja vendido
  automaticamente sem esse outro plugin nunca precisar saber o preço de nada, só
  perguntar "devo vender isso pra este jogador?". `isAutoSellActive(player)`
  (sem material) continua existindo pra compatibilidade — responde "tem auto-venda
  em qualquer capacidade" sem saber o material específico.
- Eventos próprios (`ItemSellEvent`, `PriceLookupEvent`) pra quem quiser reagir a
  vendas ou interceptar/ajustar um preço.
- Hook opcional de **PlaceholderAPI** e integração com **AlkaDrop** (coleta
  automática de drops).
- Comandos administrativos (`/alkashop reload|setprice|removeprice|debug|toggle|info`)
  pra gerenciar preços e configuração sem reiniciar o servidor.

## Dependências

- **AlkaEconomy** (hard dependency) — todas as moedas vêm de lá.
- PlaceholderAPI e AlkaDrop são soft-dependencies opcionais.

## Arquitetura

Faz parte de uma divisão em 3 plugins junto com AlkaMines e AlkaDrop: AlkaMines
cuida só de minerar/resetar blocos, AlkaDrop cuida de coleta/condensação de itens,
e o AlkaShop é o único que sabe preço — os outros dois só chamam a API dele.
