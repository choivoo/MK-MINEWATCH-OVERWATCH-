# MineWatch — 마인크래프트 속 오버워치

모드 하나만 설치하면 오버워치를 마인크래프트(큐브 블록 스타일)로 즐길 수 있는 Fabric 모드입니다.
**Minecraft 1.21.1 · Fabric Loader 0.17+ · Fabric API · Java 21**. GeckoLib 은 jar 에 포함되어 있어 따로 설치할 필요가 없습니다.

## 설치
1. Fabric Loader 0.17 이상(1.21.1)과 [Fabric API](https://modrinth.com/mod/fabric-api)를 설치합니다.
2. `minewatch-1.0.0.jar` 를 `mods` 폴더에 넣고 실행합니다.
3. 월드에 들어가면 **로비 화면**이 자동으로 열립니다(`H` 키로 다시 열기).

## 플레이 방법
| 로비 버튼 | 설명 |
|---|---|
| 게임 시작 | 내장 맵 4종(중앙 광장 · 협곡 · 요새 · 쌍탑) 중 하나가 랜덤으로 뽑혀 팀전이 시작됩니다 (호스트/OP) |
| 영웅 선택 | 공격 · 돌격 · 지원 탭에서 영웅 선택 (매치 진행 중에는 변경 불가) |
| 게임 설정 | 모드(팀 데스매치 / 점령전), 목표, 내 팀, 봇 난이도 |
| AI 대전 | 봇 상대 대전. 난이도 3단계, 부족한 인원은 아군 봇이 채움 |
| 온라인 배틀 | 서버의 매치 큐에 참가. 인원이 모이면 자동 시작 |

친구와 함께: `Esc → LAN에 열기` 후 게임 시작. 전용 서버 설정은 [docs/SERVER.md](docs/SERVER.md).

## 조작
좌클릭 사격 · **우클릭 보조 발사(조준/방벽/증폭 등)** · R 재장전 · **Shift 능력 1** · **E 능력 2** · **Q 궁극기** · V 근접(트레이서) · **G 퍽 선택** · **H 로비** (키 설정에서 변경 가능)

## 영웅 (7명)
| 역할 | 영웅 | 특징 |
|---|---|---|
| 공격 | 트레이서 | 쌍권총, 점멸, 시간 역행, 펄스 폭탄 |
| 공격 | 솔저: 76 | 펄스 소총, 질주, 헬릭스 로켓, 전술 조준경 |
| 공격 | 위도우메이커 | 저격 소총(조준·충전), 갈고리, 독 지뢰, 적외선 투시 |
| 돌격 | 라인하르트 | 망치, 정면 방벽, 돌진, 화염 강타, 대지 분쇄 |
| 돌격 | 로드호그 | 스크랩 건, 숨 돌리기, 사슬 갈고리, 돼지 대학살 |
| 지원 | 아나 | 생체 소총(치유/피해), 생체 수류탄, 수면총, 나노 강화제 |
| 지원 | 메르시 | 치유/증폭 빔, 수호천사, 발키리 |

## 시스템
- **체력**: 체력 + 방어구(노랑) + 보호막(파랑), 오버워치 수치 그대로(150 OW = 마크 체력 20). 힐팩(대 250 / 소 75).
- **퍽(오버워치 2)**: 피해·치유·처치로 경험치 → 마이너 2택1, 메이저 2택1 (영웅별 4종, 총 28개).
- **매치**: 팀 데스매치 / 점령전, 팀 스폰, 카운트다운 입력 잠금, 킬피드, 점수/타이머 HUD, 힛마커·피해 방향 표시.
- **AI 봇**: 일반 · 돌격 · 로켓 · 아군 (Seafle 봇 모델, GeckoLib).
- **사운드**: 이벤트 140종, 바닐라 소리 기반이라 저작권 오디오 없음. 리소스팩으로 교체 가능 ([docs/SOUNDS.md](docs/SOUNDS.md)).
- 월드의 지형은 건드리지 않습니다. 맵은 월드의 먼 좌표(x=20000~)에 지어지고, 매치가 끝나면 모두 원래 위치(로비)로 돌아옵니다.

## 명령어
```
/minewatch hero <영웅id|none>       영웅 선택(연습용)
/minewatch match start tdm|control  매치 시작(OP)     /minewatch match stop
/minewatch map build <맵id>         내장 맵 짓기(OP)  /minewatch map spawn a|b  /minewatch map point
/minewatch bot spawn <종류> <팀>    봇 소환(OP)       /minewatch bot clear
/minewatch healthpack large|small   힐팩 설치(OP)
/minewatch team auto|a|b            팀 선호
```

## 개발
```
powershell -File scripts\gradle.ps1 build        # 빌드 + 단위 테스트  → build/libs/minewatch-1.0.0.jar
powershell -File scripts\gradle.ps1 runGametest  # 실제 서버로 맵/봇/영웅/퍽 검증
powershell -File scripts\gradle.ps1 runClient
```
경로에 한글이 있으면 일반 `gradlew test` 가 실패하므로 `scripts/gradle.ps1`(ASCII 경로 우회)을 쓰세요.
구조와 설계는 [docs/ROADMAP.md](docs/ROADMAP.md), 가져온 에셋의 출처는 [NOTICE.md](NOTICE.md) · [docs/SEAFLE_REFERENCE.md](docs/SEAFLE_REFERENCE.md).

## 알려진 제한 (1.0)
- 무기는 큐브 JSON 모델(팔레트 텍스처)이며 GeckoLib 1인칭 팔/무기 애니메이션은 아직 없습니다.
- 새 영웅 6명의 초상화/능력 아이콘은 글자 약자입니다(트레이서만 전용 이미지).
- 클라이언트 화면(로비·영웅 선택·HUD·퍽 선택)은 제작 환경에서 눈으로 검증하지 못했습니다. 어긋남을 발견하면 이슈로 알려 주세요.
- 영웅 밸런스는 오버워치 수치를 그대로 옮긴 것이라 마인크래프트 환경에서 조정이 필요할 수 있습니다.

## 라이선스
MIT. 이식된 에셋은 Seafle Tracer(MIT), 라이브러리 GeckoLib(MIT)을 사용합니다.
