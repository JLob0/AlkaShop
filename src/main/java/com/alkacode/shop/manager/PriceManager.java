package com.alkacode.shop.manager;

import com.alkacode.shop.event.PriceLookupEvent;
import com.alkacode.shop.model.ShopPrice;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Preco global por Material. Preco direto (prices.yml) tem prioridade; se um item
 * nao tem preco direto mas seu "pai" (bloco) tem, o preco e calculado dividindo pelo
 * ratio descoberto nas receitas de crafting do servidor (ver secao 4 da
 * especificacao). Tudo pre-resolvido em {@link #resolvedCache} no load/reload para
 * acesso O(1) - {@link #getPrice} so refaz o calculo se o cache ainda nao tiver essa
 * entrada (nunca deveria acontecer apos um load() completo).
 */
public final class PriceManager {

    private final JavaPlugin plugin;

    private final Map<Material, ShopPrice> directPrices = new HashMap<>();
    private final Map<Material, Material> itemToBlock = new HashMap<>();
    private final Map<Material, Integer> itemToRatio = new HashMap<>();
    private final Map<Material, Map<String, Double>> resolvedCache = new HashMap<>();
    private final Map<Material, String> materialCategories = new HashMap<>();

    public PriceManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        directPrices.clear();
        itemToBlock.clear();
        itemToRatio.clear();
        resolvedCache.clear();
        materialCategories.clear();

        scanRecipes();
        loadDirectPrices();
        resolveAll();

        plugin.getLogger().info("AlkaShop: " + directPrices.size() + " preco(s) direto(s), "
                + itemToBlock.size() + " proporcao(oes) de crafting descoberta(s), "
                + resolvedCache.size() + " material(is) vendavel(is) no total.");
    }

    private void loadDirectPrices() {
        ConfigurationSection root = loadPricesSection();
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            Material material = Material.matchMaterial(key);
            if (material == null) {
                plugin.getLogger().warning("prices.yml: material invalido '" + key + "'.");
                continue;
            }
            ConfigurationSection entrySection = root.getConfigurationSection(key);
            if (entrySection == null) {
                continue;
            }
            Map<String, Double> currencyPrices = new LinkedHashMap<>();
            for (String currency : entrySection.getKeys(false)) {
                double value = entrySection.getDouble(currency, 0.0);
                if (value > 0) {
                    currencyPrices.put(currency.toLowerCase(), value);
                }
            }
            if (!currencyPrices.isEmpty()) {
                directPrices.put(material, new ShopPrice(material, currencyPrices));
            }
            String category = entrySection.getString("category", "");
            if (!category.isBlank()) {
                materialCategories.put(material, category.toLowerCase());
            }
        }
    }

    private ConfigurationSection loadPricesSection() {
        File file = new File(plugin.getDataFolder(), "prices.yml");
        if (!file.exists()) {
            plugin.saveResource("prices.yml", false);
        }
        return org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file).getConfigurationSection("prices");
    }

    /**
     * So considera reversivel um grid de crafting 100% preenchido com o MESMO
     * material, onde cada slot exige exatamente 1 material possivel (rejeita
     * RecipeChoice com varias opcoes, tipo "qualquer tabua" da mesa de trabalho -
     * isso evita falso-positivo tipo OAK_PLANKS x4 -> CRAFTING_TABLE, que a
     * especificacao explicitamente marca como NAO aplicavel).
     */
    private void scanRecipes() {
        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            ItemStack result = recipe.getResult();
            if (result.getAmount() != 1) {
                continue;
            }

            List<Material> ingredients = extractIngredients(recipe);
            if (ingredients == null || ingredients.isEmpty()) {
                continue;
            }
            int count = ingredients.size();
            if (count != 4 && count != 9) {
                continue;
            }
            Material first = ingredients.get(0);
            boolean allSame = ingredients.stream().allMatch(m -> m == first);
            if (!allSame || first == result.getType()) {
                continue;
            }

            itemToBlock.putIfAbsent(first, result.getType());
            itemToRatio.putIfAbsent(first, count);
        }
    }

    private List<Material> extractIngredients(Recipe recipe) {
        if (recipe instanceof ShapelessRecipe shapeless) {
            List<Material> result = new ArrayList<>();
            for (RecipeChoice choice : shapeless.getChoiceList()) {
                Material material = exactMaterial(choice);
                if (material == null) {
                    return null;
                }
                result.add(material);
            }
            return result;
        }
        if (recipe instanceof ShapedRecipe shaped) {
            List<Material> result = new ArrayList<>();
            Map<Character, RecipeChoice> choiceMap = shaped.getChoiceMap();
            for (String row : shaped.getShape()) {
                for (char c : row.toCharArray()) {
                    if (c == ' ') {
                        continue;
                    }
                    Material material = exactMaterial(choiceMap.get(c));
                    if (material == null) {
                        return null;
                    }
                    result.add(material);
                }
            }
            return result;
        }
        return null;
    }

    /** Retorna o material so se o choice representar exatamente 1 material possivel. */
    private Material exactMaterial(RecipeChoice choice) {
        if (choice instanceof RecipeChoice.MaterialChoice materialChoice) {
            List<Material> choices = materialChoice.getChoices();
            return choices.size() == 1 ? choices.get(0) : null;
        }
        if (choice instanceof RecipeChoice.ExactChoice exactChoice) {
            List<ItemStack> choices = exactChoice.getChoices();
            if (choices.isEmpty()) {
                return null;
            }
            Material first = choices.get(0).getType();
            boolean allSame = choices.stream().allMatch(item -> item.getType() == first);
            return allSame ? first : null;
        }
        return null;
    }

    private void resolveAll() {
        Set<Material> materials = new java.util.HashSet<>(directPrices.keySet());
        materials.addAll(itemToBlock.keySet());
        for (Material material : materials) {
            Map<String, Double> resolved = resolveMaterial(material);
            if (!resolved.isEmpty()) {
                resolvedCache.put(material, resolved);
            }
        }
    }

    private Map<String, Double> resolveMaterial(Material material) {
        Map<String, Double> resolved = new LinkedHashMap<>();
        ShopPrice direct = directPrices.get(material);
        if (direct != null) {
            resolved.putAll(direct.prices());
        }

        Material parent = itemToBlock.get(material);
        if (parent != null) {
            ShopPrice parentPrice = directPrices.get(parent);
            if (parentPrice != null) {
                int ratio = itemToRatio.get(material);
                for (Map.Entry<String, Double> entry : parentPrice.prices().entrySet()) {
                    resolved.putIfAbsent(entry.getKey(), entry.getValue() / ratio);
                }
            }
        }
        return resolved;
    }

    public double getPrice(Material material, String currency) {
        Map<String, Double> prices = resolvedCache.get(material);
        double price = prices != null ? prices.getOrDefault(currency.toLowerCase(), 0.0) : 0.0;

        PriceLookupEvent event = new PriceLookupEvent(material, currency.toLowerCase(), price);
        Bukkit.getPluginManager().callEvent(event);
        return event.getPrice();
    }

    public boolean isSellable(Material material) {
        Map<String, Double> prices = resolvedCache.get(material);
        return prices != null && !prices.isEmpty();
    }

    public Map<String, Double> resolvedPrices(Material material) {
        return resolvedCache.getOrDefault(material, Map.of());
    }

    public void setPrice(Material material, String currency, double value) {
        ShopPrice existing = directPrices.get(material);
        Map<String, Double> prices = existing != null ? new LinkedHashMap<>(existing.prices()) : new LinkedHashMap<>();
        prices.put(currency.toLowerCase(), value);
        directPrices.put(material, new ShopPrice(material, prices));
        persistDirectPrices();
        resolveAll();
    }

    public boolean hasDirectPrice(Material material) {
        return directPrices.containsKey(material);
    }

    /** Bloco de quem este material herda preco via ratio de crafting reversivel, ou null se ele nao e derivado de nada. */
    public Material getParent(Material material) {
        return itemToBlock.get(material);
    }

    /** Quantas unidades deste material equivalem a 1 unidade do pai (ex: 9 pro RAW_IRON -> RAW_IRON_BLOCK). 0 se nao e derivado. */
    public int getRatio(Material material) {
        return itemToRatio.getOrDefault(material, 0);
    }

    /** Precos configurados diretamente pro material (NAO inclui preco derivado via ratio de crafting). */
    public Map<String, Double> directPricesOf(Material material) {
        ShopPrice price = directPrices.get(material);
        return price != null ? price.prices() : Map.of();
    }

    /** So os materiais com entrada direta em prices.yml - o que a GUI de admin gerencia (itens derivados via ratio de crafting nao tem linha propria pra editar). */
    public Set<Material> allDirectMaterials() {
        return new HashSet<>(directPrices.keySet());
    }

    public void setCategory(Material material, String category) {
        if (category == null || category.isBlank()) {
            materialCategories.remove(material);
        } else {
            materialCategories.put(material, category.toLowerCase());
        }
        persistDirectPrices();
    }

    /** Remove o material inteiro (todas as moedas + categoria) - fica nao vendavel de novo. */
    public void removeMaterial(Material material) {
        directPrices.remove(material);
        materialCategories.remove(material);
        persistDirectPrices();
        resolveAll();
    }

    public boolean removePrice(Material material, String currency) {
        ShopPrice existing = directPrices.get(material);
        if (existing == null || !existing.prices().containsKey(currency.toLowerCase())) {
            return false;
        }
        Map<String, Double> prices = new LinkedHashMap<>(existing.prices());
        prices.remove(currency.toLowerCase());
        if (prices.isEmpty()) {
            directPrices.remove(material);
        } else {
            directPrices.put(material, new ShopPrice(material, prices));
        }
        persistDirectPrices();
        resolveAll();
        return true;
    }

    private void persistDirectPrices() {
        var yaml = new org.bukkit.configuration.file.YamlConfiguration();
        for (Map.Entry<Material, ShopPrice> entry : directPrices.entrySet()) {
            for (Map.Entry<String, Double> priceEntry : entry.getValue().prices().entrySet()) {
                yaml.set("prices." + entry.getKey().name() + "." + priceEntry.getKey(), priceEntry.getValue());
            }
            // Sem isso, o primeiro /alkashop setprice depois de qualquer material ganhar
            // uma categoria reescreveria prices.yml do zero e apagaria a categoria de
            // TODO material, ja que essa reescrita e sempre um snapshot completo do
            // directPrices, nunca um merge com o que ja estava no arquivo.
            String category = materialCategories.get(entry.getKey());
            if (category != null) {
                yaml.set("prices." + entry.getKey().name() + ".category", category);
            }
        }
        try {
            yaml.save(new File(plugin.getDataFolder(), "prices.yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("Falha ao salvar prices.yml: " + e.getMessage());
        }
    }

    public int resolvedCount() {
        return resolvedCache.size();
    }

    public Map<Material, Map<String, Double>> allResolved() {
        return resolvedCache;
    }

    /**
     * Categoria direta do material, ou - se ele nao tem uma propria - a do "pai" (bloco)
     * de quem o preco foi derivado via ratio de crafting. Sem esse fallback, um item so
     * com preco derivado (ex: RAW_IRON, que ganha preco de RAW_IRON_BLOCK) nunca aparece
     * na categoria certa (ex: "minerios") nem no menu de auto-venda nem na GUI de admin -
     * ele so teria categoria se alguem configurasse "category" nele DIRETAMENTE em
     * prices.yml, o que ninguem faz pra item derivado (o preco dele nem existe la).
     */
    public String getCategory(Material material) {
        String direct = materialCategories.get(material);
        if (direct != null) {
            return direct;
        }
        Material parent = itemToBlock.get(material);
        return parent != null ? materialCategories.getOrDefault(parent, "") : "";
    }

    /** Usa {@link #getCategory} (com fallback pro "pai") pra cada material vendavel, nao so os com categoria propria - senao /vendertudo <categoria> nao pegaria um item so com preco derivado (ex: RAW_IRON). */
    public Set<Material> getMaterialsByCategory(String category) {
        String cat = category.toLowerCase();
        Set<Material> result = new HashSet<>();
        for (Material material : resolvedCache.keySet()) {
            if (getCategory(material).equals(cat)) {
                result.add(material);
            }
        }
        return result;
    }

    public Set<String> getAllCategories() {
        return new HashSet<>(materialCategories.values());
    }
}
