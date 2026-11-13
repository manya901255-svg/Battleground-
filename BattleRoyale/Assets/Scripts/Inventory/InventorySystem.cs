using System;
using System.Collections.Generic;
using UnityEngine;

namespace BattleRoyale.Inventory
{
    public enum ItemCategory { Weapon, Ammo, Consumable, Armor, Scope, Attachment }

    [Serializable]
    public class InventoryItem
    {
        public string ItemId;
        public string DisplayName;
        public ItemCategory Category;
        public int Quantity;
        public float Weight;
    }

    public class InventorySystem : MonoBehaviour
    {
        [Header("Backpack Limits")]
        [SerializeField] private float baseCapacity = 150f;
        [SerializeField] private int backpackLevel = 1;

        public float MaxCapacity => baseCapacity + (backpackLevel * 50f);
        public float CurrentWeight { get; private set; }

        private readonly List<InventoryItem> _items = new();

        public event Action OnInventoryUpdated;

        public bool TryAddItem(InventoryItem item)
        {
            float totalWeight = CurrentWeight + (item.Weight * item.Quantity);
            if (totalWeight > MaxCapacity)
            {
                Debug.LogWarning("[Inventory] Cannot add item: Backpack weight limit reached.");
                return false;
            }

            var existing = _items.Find(i => i.ItemId == item.ItemId && i.Category != ItemCategory.Weapon);
            if (existing != null)
            {
                existing.Quantity += item.Quantity;
            }
            else
            {
                _items.Add(item);
            }

            RecalculateWeight();
            OnInventoryUpdated?.Invoke();
            return true;
        }

        public bool RemoveItem(string itemId, int quantity)
        {
            var existing = _items.Find(i => i.ItemId == itemId);
            if (existing == null || existing.Quantity < quantity) return false;

            existing.Quantity -= quantity;
            if (existing.Quantity <= 0)
            {
                _items.Remove(existing);
            }

            RecalculateWeight();
            OnInventoryUpdated?.Invoke();
            return true;
        }

        private void RecalculateWeight()
        {
            float sum = 0f;
            foreach (var item in _items) sum += item.Weight * item.Quantity;
            CurrentWeight = sum;
        }

        public IReadOnlyList<InventoryItem> GetItems() => _items.AsReadOnly();
    }
}
