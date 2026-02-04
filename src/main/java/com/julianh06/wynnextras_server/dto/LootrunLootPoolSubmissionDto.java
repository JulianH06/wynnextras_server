package com.julianh06.wynnextras_server.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class LootrunLootPoolSubmissionDto {
    private String lootrunType;
    private List<ItemDto> items;

    public LootrunLootPoolSubmissionDto() {}

    public LootrunLootPoolSubmissionDto(List<ItemDto> items) {
        this.items = items;
    }

    public String getLootrunType() {
        return lootrunType;
    }

    public void setLootrunType(String lootrunType) {
        this.lootrunType = lootrunType;
    }

    public List<ItemDto> getItems() {
        return items;
    }

    public void setItems(List<ItemDto> items) {
        this.items = items;
    }

    public static class ItemDto {
        private String name;
        private String rarity; // Mythic, Fabled, Legendary, Rare, Set, Unique
        private String type;   // normal, shiny, tome
        private String tooltip; // Full tooltip text
        private String shinyStat; // For shiny items: the stat they have (e.g., "Health", "Mana Regen")

        public ItemDto() {}

        public ItemDto(String name, String rarity, String type) {
            this.name = name;
            this.rarity = rarity;
            this.type = type;
            this.tooltip = "";
            this.shinyStat = "";
        }

        public ItemDto(String name, String rarity, String type, String tooltip, String shinyStat) {
            this.name = name;
            this.rarity = rarity;
            this.type = type;
            this.tooltip = tooltip != null ? tooltip : "";
            this.shinyStat = shinyStat != null ? shinyStat : "";
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRarity() {
            return rarity;
        }

        public void setRarity(String rarity) {
            this.rarity = rarity;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTooltip() {
            return tooltip;
        }

        public void setTooltip(String tooltip) {
            this.tooltip = tooltip != null ? tooltip : "";
        }

        public String getShinyStat() {
            return shinyStat;
        }

        public void setShinyStat(String shinyStat) {
            this.shinyStat = shinyStat != null ? shinyStat : "";
        }
    }
}
