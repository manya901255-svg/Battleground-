using System;
using UnityEngine;

namespace BattleRoyale.Player
{
    public enum MovementStance { Stand, Crouch, Prone, InVehicle }

    [RequireComponent(typeof(CharacterController))]
    public class PlayerController : MonoBehaviour
    {
        [Header("Movement Attributes")]
        [SerializeField] private float walkSpeed = 4.5f;
        [SerializeField] private float sprintSpeed = 7.2f;
        [SerializeField] private float crouchSpeed = 2.8f;
        [SerializeField] private float proneSpeed = 1.4f;
        [SerializeField] private float jumpHeight = 1.25f;
        [SerializeField] private float gravity = -19.62f;

        [Header("Vitals")]
        [SerializeField] private float maxHealth = 100f;
        [SerializeField] private float maxShield = 100f;

        public float CurrentHealth { get; private set; }
        public float CurrentShield { get; private set; }
        public MovementStance CurrentStance { get; private set; } = MovementStance.Stand;
        public bool IsAlive => CurrentHealth > 0f;

        private CharacterController _characterController;
        private Vector3 _velocity;
        private bool _isGrounded;

        public event Action<float, float> OnVitalsChanged;
        public event Action OnPlayerEliminated;

        private void Awake()
        {
            _characterController = GetComponent<CharacterController>();
            CurrentHealth = maxHealth;
            CurrentShield = maxShield;
        }

        public void ProcessMovement(Vector2 moveInput, bool isSprinting, bool isJumpPressed)
        {
            if (!IsAlive || CurrentStance == MovementStance.InVehicle) return;

            _isGrounded = _characterController.isGrounded;
            if (_isGrounded && _velocity.y < 0)
            {
                _velocity.y = -2f;
            }

            float speed = CurrentStance switch
            {
                MovementStance.Crouch => crouchSpeed,
                MovementStance.Prone => proneSpeed,
                _ => isSprinting ? sprintSpeed : walkSpeed
            };

            Vector3 move = transform.right * moveInput.x + transform.forward * moveInput.y;
            _characterController.Move(move * (speed * Time.deltaTime));

            if (isJumpPressed && _isGrounded && CurrentStance == MovementStance.Stand)
            {
                _velocity.y = Mathf.Sqrt(jumpHeight * -2f * gravity);
            }

            _velocity.y += gravity * Time.deltaTime;
            _characterController.Move(_velocity * Time.deltaTime);
        }

        public void ApplyDamage(float damage, float shieldMultiplier = 1.0f)
        {
            if (!IsAlive) return;

            if (CurrentShield > 0f)
            {
                float shieldDamage = damage * shieldMultiplier;
                if (CurrentShield >= shieldDamage)
                {
                    CurrentShield -= shieldDamage;
                }
                else
                {
                    float remainingDmg = (shieldDamage - CurrentShield) / shieldMultiplier;
                    CurrentShield = 0f;
                    CurrentHealth = Mathf.Max(0f, CurrentHealth - remainingDmg);
                }
            }
            else
            {
                CurrentHealth = Mathf.Max(0f, CurrentHealth - damage);
            }

            OnVitalsChanged?.Invoke(CurrentHealth, CurrentShield);

            if (CurrentHealth <= 0f)
            {
                OnPlayerEliminated?.Invoke();
            }
        }

        public void SetStance(MovementStance stance)
        {
            CurrentStance = stance;
            switch (stance)
            {
                case MovementStance.Stand:
                    _characterController.height = 1.8f;
                    _characterController.center = new Vector3(0, 0.9f, 0);
                    break;
                case MovementStance.Crouch:
                    _characterController.height = 1.2f;
                    _characterController.center = new Vector3(0, 0.6f, 0);
                    break;
                case MovementStance.Prone:
                    _characterController.height = 0.5f;
                    _characterController.center = new Vector3(0, 0.25f, 0);
                    break;
            }
        }
    }
}
