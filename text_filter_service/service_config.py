from pydantic_settings import BaseSettings


class ServiceConfig(BaseSettings):
    """Configuration for the Text Safety Filter Service."""

    # Model Configuration
    MODEL_PATH: str = "./YuFeng-XGuard-Reason-0.6B"  # Path to the safety model directory
    DEVICE: str = "auto"

    # Service Configuration
    HOST: str = "0.0.0.0"
    PORT: int = 8001

    # Safety threshold (0.0 ~ 1.0), scores above this are considered unsafe
    THRESHOLD: float = 0.5

    class Config:
        env_file = ".env"


service_config = ServiceConfig()

