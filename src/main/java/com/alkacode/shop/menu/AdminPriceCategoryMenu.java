package com.alkacode.shop.menu;

import com.alkacode.shop.ShopServices;
import com.alkacode.shop.util.PriceFormatter;
import com.alkacode.shop.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Grade de precos de UMA categoria (ou "outros"). Mostra os MESMOS materiais que o jogador
 * ve no menu de auto-venda pra essa categoria (allResolved) - inclusive os que so tem preco
 * DERIVADO via ratio de crafting (ex: RAW_IRON, que herda de RAW_IRON_BLOCK). Um item derivado
 * nao tem preco proprio pra editar - clicar nele redireciona pro bloco-pai, que e quem
 * realmente guarda o preco. Isso evita a GUI de admin mostrar menos itens do que a GUI do
 * jogador (o admin precisa conseguir achar/editar TUDO que o jogador ve como vendavel).
 *
 * Arraste um item do seu inventario pra um slot vazio pra adicionar como vendavel (vira
 * entrada DIRETA nova). Preco/moeda sao digitados no chat (ver AdminPriceInputManager) -
 * inventario nao tem campo de texto nativo.
 *
 * Num item DIRETO: clique esquerdo define/atualiza preco, clique direito remove uma moeda,
 * shift+clique direito remove o item inteiro (confirma clicando de novo em ate 5s, mesmo
 * padrao do editor de composicao de minas do AlkaMines). Num item DERIVADO: qualquer clique
 * abre a edicao do bloco-pai.
 */
public final class AdminPriceCategoryMenu extends AbstractShopMenu {

    private static final int ITEMS_PER_PAGE = 36;
    private static final int BACK_SLOT = 45;
    private static final int PREV_SLOT = 48;
    private static final int NEXT_SLOT = 50;
    private static final long REMOVE_CONFIRM_WINDOW_MILLIS = 5000;

    private final ShopServices services;
    private final String category;
    private final String displayName;
    private final List<Material> materials;
    private int currentPage = 0;
    private Material pendingRemoval;
    private long pendingRemovalExpiresAt;

    public AdminPriceCategoryMenu(Player viewer, ShopServices services, String category, String displayName) {
        super(viewer, TextUtil.parse("<#FFD700><b>⚙ Precos:</b></#FFD700> " + displayName), 54);
        this.services = services;
        this.category = category;
        this.displayName = displayName;
        this.materials = new ArrayList<>();
        for (Material mat : services.priceManager.allResolved().keySet()) {
            String matCategory = services.priceManager.getCategory(mat);
            boolean matches = "outros".equals(category) ? matCategory.isBlank() : matCategory.equals(category);
            if (matches) {
                materials.add(mat);
            }
        }
        materials.sort(java.util.Comparator.comparing(Material::name));
        build();
    }

    private void build() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        var fMeta = filler.getItemMeta();
        fMeta.displayName(TextUtil.parse(" "));
        filler.setItemMeta(fMeta);
        for (int i = 36; i < 54; i++) {
            if (i != BACK_SLOT && i != PREV_SLOT && i != NEXT_SLOT) {
                setItem(i, filler);
            }
        }

        ItemStack back = new ItemStack(Material.ARROW);
        var bMeta = back.getItemMeta();
        bMeta.displayName(TextUtil.parse("<#FFD700>◀ Voltar ao painel"));
        back.setItemMeta(bMeta);
        setItem(BACK_SLOT, back, e -> {
            viewer.closeInventory();
            new AdminPriceHubMenu(viewer, services).open();
        });

        renderPage();
    }

    @Override
    public boolean allowsBottomInventoryClicks() {
        return true;
    }

    private void renderPage() {
        int totalPages = Math.max(1, (materials.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        if (currentPage >= totalPages) {
            currentPage = totalPages - 1;
        }

        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, materials.size());

        for (int i = 0; i < 36; i++) {
            int index = start + i;
            if (index < end) {
                setItem(i, buildMaterialIcon(materials.get(index)), clickHandlerFor(materials.get(index)));
            } else {
                setItem(i, null, e -> {
                    ItemStack cursor = e.getCursor();
                    if (cursor == null || cursor.getType().isAir()) {
                        return;
                    }
                    handleAddMaterial(cursor.getType());
                });
            }
        }

        if (currentPage > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            var pMeta = prev.getItemMeta();
            pMeta.displayName(TextUtil.parse("<yellow>◀ Pagina anterior"));
            prev.setItemMeta(pMeta);
            setItem(PREV_SLOT, prev, e -> {
                currentPage--;
                renderPage();
            });
        } else {
            setItem(PREV_SLOT, null, null);
        }

        if (currentPage < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            var nMeta = next.getItemMeta();
            nMeta.displayName(TextUtil.parse("<yellow>Proxima pagina ▶"));
            next.setItemMeta(nMeta);
            setItem(NEXT_SLOT, next, e -> {
                currentPage++;
                renderPage();
            });
        } else {
            setItem(NEXT_SLOT, null, null);
        }
    }

    private ItemStack buildMaterialIcon(Material mat) {
        String cat = services.priceManager.getCategory(mat);
        String catDisplay = cat.isBlank() ? "Nenhuma" : cat.substring(0, 1).toUpperCase() + cat.substring(1);
        boolean direct = services.priceManager.hasDirectPrice(mat);
        Material parent = services.priceManager.getParent(mat);

        ItemStack icon = new ItemStack(mat);
        var meta = icon.getItemMeta();
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();

        if (direct) {
            meta.displayName(TextUtil.parse("<#FFD700>" + mat.name()));
            lore.add(TextUtil.parse("<gray>Categoria: <white>" + catDisplay));
            for (Map.Entry<String, Double> entry : services.priceManager.directPricesOf(mat).entrySet()) {
                lore.add(TextUtil.parse("<gray>✦ <white>" + entry.getKey() + "</white>: <yellow>"
                        + PriceFormatter.format(entry.getValue(), true, 2)));
            }
            lore.add(TextUtil.parse(" "));
            lore.add(TextUtil.parse("<yellow>Clique esquerdo: <white>definir/atualizar preco"));
            lore.add(TextUtil.parse("<yellow>Clique direito: <white>remover uma moeda"));
            lore.add(TextUtil.parse("<yellow>Shift+direito: <white>remover o item inteiro"));
        } else {
            meta.displayName(TextUtil.parse("<gray>" + mat.name() + " <dark_gray>(herdado)"));
            lore.add(TextUtil.parse("<gray>Categoria: <white>" + catDisplay));
            for (Map.Entry<String, Double> entry : services.priceManager.resolvedPrices(mat).entrySet()) {
                lore.add(TextUtil.parse("<gray>✦ <white>" + entry.getKey() + "</white>: <yellow>"
                        + PriceFormatter.format(entry.getValue(), true, 2) + " <dark_gray>(herdado)"));
            }
            lore.add(TextUtil.parse(" "));
            if (parent != null) {
                lore.add(TextUtil.parse("<gray>Preco herdado de <white>" + parent.name() + "</white>."));
            }
            lore.add(TextUtil.parse("<yellow>Clique em qualquer botao: <white>editar o preco do bloco-pai"));
        }
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.values());
        icon.setItemMeta(meta);
        return icon;
    }

    private java.util.function.Consumer<org.bukkit.event.inventory.InventoryClickEvent> clickHandlerFor(Material mat) {
        boolean direct = services.priceManager.hasDirectPrice(mat);
        if (!direct) {
            Material parent = services.priceManager.getParent(mat);
            return e -> {
                if (parent == null) {
                    return;
                }
                viewer.sendMessage(TextUtil.parse(services.configManager.prefix()
                        + "<yellow>" + mat.name() + " herda preco de <white>" + parent.name() + "</white> - editando o bloco-pai."));
                services.adminPriceInput.promptSetPrice(viewer, parent, category, displayName, false);
            };
        }
        return e -> {
            switch (e.getClick()) {
                case LEFT -> services.adminPriceInput.promptSetPrice(viewer, mat, category, displayName, false);
                case RIGHT -> services.adminPriceInput.promptRemoveCurrency(viewer, mat, category, displayName);
                case SHIFT_RIGHT -> handleRemoveRequest(mat);
                default -> {
                    // duplo-clique, tecla de numero, etc. - nao fazem sentido aqui, ignora.
                }
            }
        };
    }

    private void handleAddMaterial(Material material) {
        if (services.priceManager.hasDirectPrice(material)) {
            viewer.sendMessage(TextUtil.parse(services.configManager.prefix()
                    + "<red>" + material.name() + " ja esta configurado - clique nele pra editar."));
            return;
        }
        Material parent = services.priceManager.getParent(material);
        if (parent != null) {
            viewer.sendMessage(TextUtil.parse(services.configManager.prefix()
                    + "<yellow>" + material.name() + " ja e vendavel - herda preco de <white>" + parent.name()
                    + "</white>. Editando o bloco-pai (ou continue pra dar um preco proprio a ele, que passa a ter prioridade)."));
        }
        services.adminPriceInput.promptSetPrice(viewer, material, category, displayName, true);
    }

    /** Shift+clique direito exige confirmar clicando de novo no mesmo item em ate 5s. */
    private void handleRemoveRequest(Material material) {
        if (pendingRemoval == material && pendingRemovalExpiresAt > System.currentTimeMillis()) {
            pendingRemoval = null;
            services.priceManager.removeMaterial(material);
            viewer.sendMessage(TextUtil.parse(services.configManager.prefix() + "<red>" + material.name() + " removido - nao e mais vendavel."));
            viewer.closeInventory();
            new AdminPriceCategoryMenu(viewer, services, category, displayName).open();
            return;
        }

        pendingRemoval = material;
        pendingRemovalExpiresAt = System.currentTimeMillis() + REMOVE_CONFIRM_WINDOW_MILLIS;
        viewer.sendMessage(TextUtil.parse(services.configManager.prefix()
                + "<yellow>Shift+clique direito de novo em ate 5s pra confirmar a remocao de <white>" + material.name() + "</white>."));
    }
}
