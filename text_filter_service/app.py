import logging
from contextlib import asynccontextmanager
from typing import Optional, Tuple

import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from service_config import service_config
from api_models import TextCheckRequest, TextCheckResponse
from text_classifier import classifier

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan — model is already loaded via text_classifier module."""
    logger.info("Text Safety Filter Service is ready.")
    yield


app = FastAPI(
    title="Text Safety Filter Service",
    description="Detects safety risks in text using a local LLM guard model.",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


def evaluate_risk(risk_map: dict, threshold: float) -> Tuple[bool, Optional[str], float]:
    """Determine the highest-risk category and whether it exceeds the threshold.

    Args:
        risk_map: Mapping of category code -> probability score.
        threshold: Score above which the text is considered unsafe.

    Returns:
        (is_safe, blocked_category_or_None, max_score)
    """
    max_score = 0.0
    blocked_cat: Optional[str] = None

    for category, score in risk_map.items():
        if category == "sec":
            continue  # Skip the "safe" category

        if score > max_score:
            max_score = score
            blocked_cat = category

    if max_score >= threshold:
        return False, blocked_cat, max_score

    return True, None, max_score


# ------------------------------------------------------------------
# Endpoints
# ------------------------------------------------------------------

@app.get("/health")
async def health():
    """Simple health-check endpoint."""
    return {"status": "ok"}


@app.post("/api/check", response_model=TextCheckResponse)
async def check_text(request: TextCheckRequest):
    """Check a single text for safety risks."""
    try:
        risk_map = classifier.classify(request.text)
    except Exception:
        logger.exception("Model inference failed")
        return TextCheckResponse(
            is_safe=False,
            risk_category="model_error",
            risk_score=1.0,
            risk_details={},
        )

    threshold = request.threshold if request.threshold is not None else service_config.THRESHOLD
    is_safe, blocked_cat, score = evaluate_risk(risk_map, threshold)

    return TextCheckResponse(
        is_safe=is_safe,
        risk_category=blocked_cat,
        risk_score=score,
        risk_details=risk_map,
    )


if __name__ == "__main__":
    uvicorn.run(app, host=service_config.HOST, port=service_config.PORT)

