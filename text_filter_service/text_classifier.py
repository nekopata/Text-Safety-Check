import logging
from pathlib import Path
from typing import Dict

import torch
from transformers import AutoModelForCausalLM, AutoTokenizer

from service_config import service_config

logger = logging.getLogger(__name__)

# Valid risk category tokens aligned with the model's classification system.
# See frontend/src/utils/risk_defs.js for the full human-readable mapping.
VALID_CATEGORIES = frozenset([
    "dw", "pc", "dc", "pi", "ec", "ac", "def", "ti", "cy",
    "ph", "mh", "se", "sci", "pp", "cs", "acc", "mc", "ha",
    "ps", "ter", "sd", "ext", "fin", "med", "law", "cm", "ma", "md", "sec",
])

# Number of top probability tokens to inspect for risk categories
TOP_K = 20


class TextClassifier:
    """Loads the safety-guard LLM and classifies text into risk categories."""

    def __init__(self, model_path: str, device: str = "auto"):
        resolved_path = str(Path(model_path).resolve())
        logger.info("Loading model from %s ...", resolved_path)
        self.tokenizer = AutoTokenizer.from_pretrained(resolved_path)
        self.model = AutoModelForCausalLM.from_pretrained(
            resolved_path,
            torch_dtype="auto",
            device_map=device,
        ).eval()
        self.id2risk: Dict[str, str] = self.tokenizer.init_kwargs.get("id2risk", {})

        logger.info("Model loaded successfully.")

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def classify(self, text: str, max_new_tokens: int = 1) -> Dict[str, float]:
        """Classify a single text and return a risk-score map."""
        messages = [{"role": "user", "content": text}]
        rendered = self.tokenizer.apply_chat_template(
            messages, tokenize=False, add_generation_prompt=True,
        )
        model_inputs = self.tokenizer(
            [rendered], return_tensors="pt",
        ).to(self.model.device)

        with torch.no_grad():
            outputs = self.model.generate(
                **model_inputs,
                max_new_tokens=max_new_tokens,
                do_sample=False,
                output_scores=True,
                return_dict_in_generate=True,
            )

        first_token_probs = outputs.scores[0][0].softmax(-1)
        return self._extract_risk_scores(first_token_probs)

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    def _extract_risk_scores(self, probs: torch.Tensor) -> Dict[str, float]:
        """Extract risk category scores from the top-k token probabilities."""
        risk_score_map: Dict[str, float] = {}
        values, indices = probs.topk(k=TOP_K)

        for value, idx in zip(values, indices):
            token_str = self.tokenizer.decode([idx.item()]).strip()
            if token_str in risk_score_map:
                continue

            score = value.item()
            if token_str in self.id2risk:
                risk_score_map[self.id2risk[token_str]] = score
            elif token_str in VALID_CATEGORIES:
                risk_score_map[token_str] = score

        return risk_score_map


def _create_classifier() -> TextClassifier:
    """Create the global TextClassifier instance."""
    try:
        return TextClassifier(
            model_path=service_config.MODEL_PATH,
            device=service_config.DEVICE,
        )
    except Exception:
        logger.exception("Failed to load safety model from '%s'", service_config.MODEL_PATH)
        raise


classifier = _create_classifier()

