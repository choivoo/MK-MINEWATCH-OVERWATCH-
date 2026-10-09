# Seafle Tracer(1.0.0, 1.20.1) 기준 자료

사용자가 제공한 이전 작업물 `seafle-tracer-1.0.0.jar`(작성자 표기 Seafle, MIT, GeckoLib 4.4.4 의존)을 분석한 결과.
이 프로젝트의 비주얼/기능 기준으로 삼는다. **코드는 난독화된 1.20.1용 바이트코드라 직접 재사용하지 않고**,
에셋과 설계를 1.21.1(Yarn) 프로젝트로 단계적으로 이식한다. 이식한 에셋에는 원 라이선스(MIT, Seafle) 표기를 유지한다.

## 보유 에셋 (namespace `tracer`)
| 종류 | 개수 | 내용 | 이식 시점 |
|---|---|---|---|
| textures/gui/hud | 161 | 능력 아이콘(add/mul 합성 쌍), 탄약선, 배너, 폭탄/봇 마커 등 | HUD 이식 단계 |
| textures/gui/pick | 158 | 영웅 선택 로스터, 흉상, 프레임, 하단 힌트 | 영웅 선택 UI 이식 |
| textures/gui/perk | 58 | 퍽 아이콘/배지 | 퍽 시스템(M8) |
| textures/fx | 174 | 블링크 라인, 폭탄 이펙트, 봇 이펙트 | 이펙트 이식 |
| geo / animations | 8 / 7 | 펄스 쌍권총, 1인칭 팔, 펄스 폭탄, 봇 4종 (GeckoLib) | 모델/애니(M8) |
| shaders/core | 27 | fx_add/blend/premul/screen, hero/bot 마스크·외곽선, tp_ghost/holo/lens | 이펙트 이식 |
| font | 9 | Barlow Condensed, Jost, 이름용 폰트 (OFL) | HUD 이식 |
| voxels | 6 | 힐팩/궁극기팩 복셀 모델 | 힐팩 엔티티화 |

## 기능 목록(클래스/믹스인/언어 파일로 확인)
펄스 쌍권총, 점멸, 시간 역행, 펄스 폭탄, 훈련장 봇(일반/돌격/로켓/아군), 생명력 팩 대·소, 궁극기팩,
특전(퍽) 선택 키, 영웅 선택 키, 사망 카메라/래그돌, 카메라 흔들림, 3인칭 포즈/숨김/고스트 렌더,
영웅 외곽선/네임태그, 틱 시계(TickClock) 연출.

## 이식 계획
- M5(AI 대전): 봇 4종 geo/animation/texture 이식 + 서버측 봇 로직
- HUD/영웅 선택 UI: gui 텍스처 + 폰트 + 합성 셰이더 이식
- M8: GeckoLib(1.21.1 대응 버전) 도입 후 geo/animation 이식, 퍽 UI, 사운드
