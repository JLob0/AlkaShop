# AlkaShop

> Sistema universal de vendas, com preços justos calculados via receitas de crafting

![Java](https://img.shields.io/badge/Java-21-orange)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-green)
![Version](https://img.shields.io/badge/Version-1.0.13-blue)
![License](https://img.shields.io/badge/License-Proprietary-red)

---

## 📋 Sobre o Projeto

O **AlkaShop** é o sistema de vendas da rede AlkaStudio: preços globais por
material, cálculo inteligente de proporções via receitas de crafting (um
bloco vale 9× o minério, por exemplo) e suporte a múltiplas moedas via
AlkaEconomy. É o único plugin da rede que sabe preço — AlkaMines e AlkaDrop
apenas consultam a API dele.

## ✨ Funcionalidades Principais

- 🛒 **GUI de vendas**: venda por categoria, inventário inteiro ou item na mão.
- 📦 **Baú virtual de venda selecionada**: escolha exatamente o que vender
  antes de confirmar.
- ⚙️ **Venda automática configurável**: ativa por material ou categoria,
  benefício de VIP.
- 🧮 **Preços calculados por receita**: proporções derivadas das receitas de
  crafting do próprio servidor, sem precisar cadastrar preço um por um.
- 💰 **Múltiplas moedas**: preços e pagamentos via AlkaEconomy.
- 🔍 **Consulta de preço**: veja o valor de qualquer material a qualquer momento.

## 🎮 Comandos

| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/vender` | Abre a GUI principal de vendas | `alkashop.use` |
| `/vendertudo [categoria]` | Vende todo o inventário, ou só uma categoria | `alkashop.use` |
| `/vendermao` | Vende o item na mão principal | `alkashop.use` |
| `/vendersel` | Abre o baú virtual de venda selecionada | `alkashop.use` |
| `/venderautomatico [todos\|nenhum\|<material>]` (`autosell`) | Configura a venda automática por material/categoria (VIP) | `alkashop.autosell` |
| `/venderpreco <material>` | Vê o preço de um material | `alkashop.use` |
| `/alkashop <reload\|setprice\|removeprice\|gui\|debug\|toggle\|info>` | Comando administrativo | `alkashop.admin.reload` |

## 🔗 Integrações

Construído sobre o **AlkaEconomy**. Parte de uma divisão em 3 plugins junto
com **AlkaMines** (mineração) e **AlkaDrop** (coleta/condensação) — os dois
consomem a API do AlkaShop para vender automaticamente. Integra-se também
com **AlkaVips** (limites de auto-venda por tier), **AlkaRankUp** e
**PlaceholderAPI**.

## 🔧 Tecnologias Utilizadas

- **Java 21**
- **Paper API 1.21.8**
- **AlkaEconomy**
- **MiniMessage** para formatação de texto

## ⚙️ Instalação

1. Baixe o `AlkaShop.jar` mais recente.
2. Coloque na pasta `plugins/` do servidor.
3. Certifique-se de ter o **AlkaEconomy** instalado (dependência obrigatória).
4. Reinicie o servidor.
5. Ajuste preços em `plugins/AlkaShop/prices.yml` ou via `/alkashop setprice`.

## 🔐 Permissões

| Permissão | Descrição | Padrão |
|-----------|-----------|--------|
| `alkashop.use` | Permite usar comandos de venda | true |
| `alkashop.autosell` | Permite ativar venda automática (VIP) | false |
| `alkashop.autosell.all` | Auto-venda para todos os itens de uma vez | false |
| `alkashop.autosell.minerios` | Auto-vender materiais da categoria minérios | false |
| `alkashop.autosell.fazenda` | Auto-vender materiais da categoria fazenda | false |
| `alkashop.autosell.madeira` | Auto-vender materiais da categoria madeira | false |
| `alkashop.admin.reload` | Recarregar config/preços | op |
| `alkashop.admin.setprice` | Definir/remover preços | op |
| `alkashop.admin.debug` | Modo debug | op |
| `alkashop.admin.others` | Controlar configuração de outros jogadores | op |
| `alkashop.admin.info` | Ver estatísticas de outros jogadores | op |

## 📝 Licença

> ⚠️ **Projeto proprietário da AlkaStudio.**
>
> Código fonte destinado exclusivamente ao uso interno da rede `Alka*`.
> Reprodução, distribuição ou uso não autorizado não são permitidos.

## 🎯 Créditos

- **Desenvolvido por**: MestreDEV — AlkaStudio
- **Parte do ecossistema**: `Alka*`

---

<div align="center">

**Desenvolvido com ❤️ pela AlkaStudio**

[![AlkaStudio](https://img.shields.io/badge/AlkaStudio-JLob0-blue)](https://github.com/JLob0)

</div>
