package com.julianh06.wynnextras_server.dto;

import java.util.List;

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
        private String rarity;
        private String type; // normal, shiny, tome

        public ItemDto() {}

        public ItemDto(String name, String rarity, String type) {
            this.name = name;
            this.rarity = rarity;
            this.type = type;
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
    }
}
