from typing import Dict, Optional

from pydantic import BaseModel


class TextCheckRequest(BaseModel):
    """Request body for single text safety check."""
    text: str
    threshold: Optional[float] = None


class TextCheckResponse(BaseModel):
    """Response body for a single text safety check result."""
    is_safe: bool
    risk_category: Optional[str] = None
    risk_score: float = 0.0
    risk_details: Dict[str, float] = {}

