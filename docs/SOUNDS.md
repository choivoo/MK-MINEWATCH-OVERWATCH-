# 사운드와 리소스팩

MineWatch 의 모든 효과음은 `assets/minewatch/sounds.json` 의 이벤트(약 140개)로 등록된다.
기본값은 **바닐라 마인크래프트 소리를 피치/볼륨만 바꿔 쓴 것**이라 별도 오디오 파일(저작권)이 없다.

## 소리 바꾸기 (리소스팩)
1. 리소스팩에 `assets/minewatch/sounds.json` 을 만들고 바꿀 이벤트만 적는다.
2. 오디오 파일은 `assets/minewatch/sounds/<이름>.ogg` 에 넣고 `"name": "minewatch:<이름>"` 으로 가리킨다.

```json
{
  "pulse_fire_crack": { "replace": true, "sounds": [ { "name": "minewatch:my_gun_shot", "volume": 0.6 } ] }
}
```

## 코드에서 재생하기
`Sfx.at(entity|world,...)`(주변 모두) / `Sfx.play(player,...)`(본인만). 이벤트 이름은 sounds.json 키와 같다.
새 이벤트를 코드에서 쓰면 `SoundsTest` 의 `USED` 목록에도 추가한다(누락 검사).

## 모델
무기는 `tools/GenModels.java` 가 만든 큐브 기반 JSON 모델이다(팔레트 텍스처 1장).
`java tools/GenModels.java src/main/resources/assets/minewatch` 로 다시 생성한다.
