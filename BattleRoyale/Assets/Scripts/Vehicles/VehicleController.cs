using System;
using System.Collections.Generic;
using UnityEngine;

namespace BattleRoyale.Vehicles
{
    public enum VehicleCategory { Sedan, ArmoredJeep, Buggy, Bike }

    public class VehicleController : MonoBehaviour
    {
        [Header("Engine & Handling")]
        [SerializeField] private VehicleCategory category = VehicleCategory.ArmoredJeep;
        [SerializeField] private float maxMotorTorque = 850f;
        [SerializeField] private float maxSteerAngle = 32f;
        [SerializeField] private float brakeTorque = 2200f;
        [SerializeField] private float maxHealth = 750f;
        [SerializeField] private int seatCount = 4;

        [Header("Wheel Colliders")]
        [SerializeField] private WheelCollider frontLeftWheel;
        [SerializeField] private WheelCollider frontRightWheel;
        [SerializeField] private WheelCollider rearLeftWheel;
        [SerializeField] private WheelCollider rearRightWheel;

        public float CurrentHealth { get; private set; }
        public bool IsDestroyed => CurrentHealth <= 0f;
        public string DriverPlayerId { get; private set; }
        public List<string> PassengerPlayerIds { get; private set; } = new();

        public event Action OnVehicleDestroyed;

        private void Awake()
        {
            CurrentHealth = maxHealth;
        }

        public void ApplyDriverInput(float throttle, float steer, bool isBraking)
        {
            if (IsDestroyed) return;

            float motor = throttle * maxMotorTorque;
            float steering = steer * maxSteerAngle;

            if (frontLeftWheel != null && frontRightWheel != null)
            {
                frontLeftWheel.steerAngle = steering;
                frontRightWheel.steerAngle = steering;
            }

            if (rearLeftWheel != null && rearRightWheel != null)
            {
                rearLeftWheel.motorTorque = motor;
                rearRightWheel.motorTorque = motor;

                float bTorque = isBraking ? brakeTorque : 0f;
                rearLeftWheel.brakeTorque = bTorque;
                rearRightWheel.brakeTorque = bTorque;
            }
        }

        public bool TryEnter(string playerId, out bool isDriver)
        {
            isDriver = false;
            if (IsDestroyed) return false;

            if (string.IsNullOrEmpty(DriverPlayerId))
            {
                DriverPlayerId = playerId;
                isDriver = true;
                return true;
            }
            else if (PassengerPlayerIds.Count < seatCount - 1)
            {
                PassengerPlayerIds.Add(playerId);
                return true;
            }

            return false;
        }

        public void Exit(string playerId)
        {
            if (DriverPlayerId == playerId)
            {
                DriverPlayerId = null;
                ApplyDriverInput(0f, 0f, true);
            }
            else
            {
                PassengerPlayerIds.Remove(playerId);
            }
        }

        public void ApplyDamage(float damage)
        {
            if (IsDestroyed) return;

            CurrentHealth = Mathf.Max(0f, CurrentHealth - damage);
            if (CurrentHealth <= 0f)
            {
                OnVehicleDestroyed?.Invoke();
            }
        }
    }
}
