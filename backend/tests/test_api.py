from types import SimpleNamespace

import pytest
from fastapi.testclient import TestClient

from app.main import Settings, app, get_client, get_settings

PNG = b"\x89PNG\r\n\x1a\n" + b"0" * 32

VALID_JSON = """{
  "readable": true,
  "medications": [{
    "name": "Losartán", "dose": "50 mg", "times": ["08:00", "20:00"],
    "times_are_suggested": true, "duration_days": 30,
    "instructions": "Tomar con agua", "confidence": "high", "notes_for_reviewer": ""
  }],
  "warnings": []
}"""


class FakeMessages:
    def __init__(self, response):
        self.response = response
        self.calls = []

    def create(self, **kwargs):
        self.calls.append(kwargs)
        return self.response


def fake_client(text=VALID_JSON, stop_reason="end_turn"):
    response = SimpleNamespace(
        stop_reason=stop_reason,
        stop_details=None,
        content=[SimpleNamespace(type="text", text=text)],
        _request_id="req_test",
    )
    messages = FakeMessages(response)
    return SimpleNamespace(beta=SimpleNamespace(messages=messages)), messages


@pytest.fixture
def client():
    yield TestClient(app)
    app.dependency_overrides.clear()


def use(fake, api_key=""):
    app.dependency_overrides[get_client] = lambda: fake
    app.dependency_overrides[get_settings] = lambda: Settings(app_api_key=api_key)


def post(client, data=PNG, content_type="image/png", headers=None):
    return client.post(
        "/v1/prescriptions/extract",
        files={"image": ("receta.png", data, content_type)},
        headers=headers or {},
    )


def test_extracts_medications(client):
    fake, messages = fake_client()
    use(fake)
    r = post(client)
    assert r.status_code == 200
    med = r.json()["medications"][0]
    assert med["name"] == "Losartán"
    assert med["times"] == ["08:00", "20:00"]
    call = messages.calls[0]
    assert call["model"] == "claude-opus-5-5"
    assert call["fallbacks"] == "default"
    assert call["output_config"]["format"]["type"] == "json_schema"


def test_rejects_non_image(client):
    use(fake_client()[0])
    assert post(client, b"hello", "text/plain").status_code == 415


def test_rejects_oversized_image(client):
    use(fake_client()[0])
    assert post(client, b"0" * (5 * 1024 * 1024 + 1)).status_code == 413


def test_requires_api_key_when_configured(client):
    use(fake_client()[0], api_key="secret")
    assert post(client).status_code == 401
    assert post(client, headers={"X-API-Key": "secret"}).status_code == 200


def test_refusal_returns_422(client):
    use(fake_client(stop_reason="refusal")[0])
    assert post(client).status_code == 422


def test_invalid_model_json_returns_502(client):
    use(fake_client(text='{"readable": true}')[0])
    assert post(client).status_code == 502


def test_schema_is_closed():
    from app.extraction import _SCHEMA

    assert _SCHEMA["additionalProperties"] is False
    assert _SCHEMA["$defs"]["Medication"]["additionalProperties"] is False


def test_defaults_to_spanish(client):
    fake, messages = fake_client()
    use(fake)
    post(client)
    call = messages.calls[0]
    assert "in Spanish" in call["system"]
    assert call["messages"][0]["content"][1]["text"] == "Transcribe esta receta."


def test_english_request_asks_for_english_output(client):
    fake, messages = fake_client()
    use(fake)
    post(client, headers={"Accept-Language": "en-US,en;q=0.9"})
    call = messages.calls[0]
    assert "in English" in call["system"]
    assert call["messages"][0]["content"][1]["text"] == "Transcribe this prescription."


@pytest.mark.parametrize(
    "header, expected",
    [("en", "The image must be JPEG, PNG, WEBP or GIF."), ("es-MX", "La imagen debe ser JPEG, PNG, WEBP o GIF.")],
)
def test_errors_follow_language(client, header, expected):
    use(fake_client()[0])
    r = post(client, b"hello", "text/plain", headers={"Accept-Language": header})
    assert r.json()["detail"] == expected


def test_refusal_message_in_english(client):
    use(fake_client(stop_reason="refusal")[0])
    r = post(client, headers={"Accept-Language": "en"})
    assert r.json()["detail"] == "This prescription could not be read. Try another photo."


def test_every_message_has_both_languages():
    from app.i18n import LANGUAGE_NAMES, MESSAGES

    for key, texts in MESSAGES.items():
        assert set(texts) == set(LANGUAGE_NAMES), key


@pytest.mark.parametrize(
    "header, expected",
    [(None, "es"), ("", "es"), ("en", "en"), ("en-GB,es;q=0.5", "en"), ("es-419", "es"), ("fr-FR", "es")],
)
def test_language_from_header(header, expected):
    from app.i18n import language_from_header

    assert language_from_header(header) == expected


def test_anthropic_key_is_secret():
    settings = Settings(anthropic_api_key="sk-ant-test-value")
    assert "sk-ant-test-value" not in repr(settings)
    assert "sk-ant-test-value" not in str(settings.model_dump())


def test_refuses_to_start_on_cloud_run_without_access_key(monkeypatch):
    monkeypatch.setenv("K_SERVICE", "pill-reminder-backend")
    monkeypatch.setenv("APP_API_KEY", "")
    get_settings.cache_clear()
    try:
        with pytest.raises(RuntimeError, match="APP_API_KEY"):
            with TestClient(app):
                pass
    finally:
        get_settings.cache_clear()


def test_starts_on_cloud_run_with_access_key(monkeypatch):
    monkeypatch.setenv("K_SERVICE", "pill-reminder-backend")
    monkeypatch.setenv("APP_API_KEY", "some-key")
    get_settings.cache_clear()
    try:
        with TestClient(app) as c:
            assert c.get("/health").status_code == 200
    finally:
        get_settings.cache_clear()
