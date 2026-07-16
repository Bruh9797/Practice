import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

async function findArtifactUtils() {
  if (process.env.PRESENTATIONS_SKILL_DIR) {
    return path.join(process.env.PRESENTATIONS_SKILL_DIR, 'container_tools', 'artifact_tool_utils.mjs');
  }
  const versionsRoot = path.join(
    os.homedir(), '.codex', 'plugins', 'cache', 'openai-primary-runtime', 'presentations',
  );
  const versions = (await fs.readdir(versionsRoot, { withFileTypes: true }))
    .filter((entry) => entry.isDirectory())
    .map((entry) => entry.name)
    .sort((a, b) => b.localeCompare(a, undefined, { numeric: true }));
  if (!versions.length) throw new Error('Не найден установленный presentations plugin');
  return path.join(versionsRoot, versions[0], 'skills', 'presentations', 'container_tools', 'artifact_tool_utils.mjs');
}

const {
  ensureArtifactToolWorkspace,
  importArtifactTool,
  saveBlobToFile,
} = await import(pathToFileURL(await findArtifactUtils()).href);

const workspace = path.resolve(process.argv[2]);
const outputPptx = path.resolve(process.argv[3]);
const projectRoot = path.resolve(process.argv[4] || process.cwd());
const starterPptxPath = path.join(workspace, 'template-starter.pptx');
const starterLayoutDir = path.join(workspace, 'template-starter-layout');
const finalSlidesDir = path.join(workspace, 'final-slides');
const finalLayoutDir = path.join(workspace, 'final-layout');

await ensureArtifactToolWorkspace(workspace);
const { FileBlob, PresentationFile } = await importArtifactTool(workspace);
const presentation = await PresentationFile.importPptx(await FileBlob.load(starterPptxPath));

const layouts = new Map();
for (let slide = 1; slide <= 19; slide += 1) {
  const file = path.join(starterLayoutDir, `starter-slide-${String(slide).padStart(2, '0')}.layout.json`);
  layouts.set(slide, JSON.parse(await fs.readFile(file, 'utf8')));
}

function layoutElement(slideNumber, predicate) {
  const match = layouts.get(slideNumber).elements.find(predicate);
  if (!match) throw new Error(`Missing mapped element on slide ${slideNumber}`);
  return match;
}

function resolvedElement(slideNumber, predicate) {
  const mapped = layoutElement(slideNumber, predicate);
  const slide = presentation.slides.items[slideNumber - 1];
  const candidates = slide.elements.items.filter((element) => element.type === mapped.kind);
  const match = candidates.find((element) => (
    mapped.text && element.text?.toString?.() === mapped.text
  )) || candidates.find((element) => mapped.name && element.name === mapped.name) || candidates[0];
  if (!match) throw new Error(`Cannot resolve ${mapped.kind} on slide ${slideNumber}`);
  return match;
}

function rewrite(slideNumber, predicate, value) {
  const element = layoutElement(slideNumber, predicate);
  const target = resolvedElement(slideNumber, predicate);
  if (!element.text) throw new Error(`Mapped text is empty on slide ${slideNumber}: ${element.aid}`);
  target.text.set(value);
}

function rewriteFooter(slideNumber) {
  rewrite(
    slideNumber,
    (element) => element.text?.startsWith('Программный комплекс для поиска, фильтрации'),
    'ThermoSelect — информационно-поисковая система выбора теплообменников',
  );
}

async function imageBytes(relativePath) {
  const bytes = await fs.readFile(path.join(projectRoot, relativePath));
  return bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength);
}

async function replaceImage(slideNumber, predicate, relativePath, alt, options = {}) {
  const image = resolvedElement(slideNumber, predicate);
  const frame = options.frame || image.frame;
  const contentType = path.extname(relativePath).toLowerCase() === '.svg' ? 'image/svg+xml' : 'image/png';
  await image.replace({
    blob: await imageBytes(relativePath),
    contentType,
    alt,
    fit: options.fit || 'contain',
  });
  image.position = frame;
  image.fit = options.fit || 'contain';
  image.crop = options.crop || { left: 0, top: 0, right: 0, bottom: 0 };
  image.geometry = 'rect';
}

async function addImage(slideNumber, relativePath, alt, position) {
  const slide = presentation.slides.items[slideNumber - 1];
  slide.images.add({
    blob: await imageBytes(relativePath),
    contentType: 'image/png',
    alt,
    fit: 'contain',
    position,
  });
}

function setTable(slideNumber, values) {
  const table = resolvedElement(slideNumber, (element) => element.kind === 'table');
  values.forEach((row, rowIndex) => row.forEach((value, columnIndex) => {
    table.cells.set(rowIndex, columnIndex, value);
  }));
}

rewrite(1, (e) => e.text === 'Санкт-Петербург 2026', 'Санкт-Петербург 2026');
rewrite(1, (e) => e.text === 'УЧЕБНАЯ ПРАКТИКА', 'УЧЕБНАЯ ПРАКТИКА');
rewrite(
  1,
  (e) => e.text?.startsWith('УГСН 09.00.00'),
  'УГСН 09.00.00 «Информатика и вычислительная техника»\nНаправление подготовки: 09.03.01 «Информатика и вычислительная техника»\nНаправленность: Системы автоматизированного проектирования\nУровень подготовки: бакалавр\nФорма обучения: очная\nФакультет информационных технологий и управления\nКафедра систем автоматизированного проектирования и управления',
);
rewrite(
  1,
  (e) => e.text?.startsWith('«Разработка информационно-поисковой системы'),
  '«Разработка информационно-поисковой системы ThermoSelect для выбора теплообменных аппаратов»',
);
setTable(1, [
  ['Обучающийся', 'Студент гр. __________  ______________________________'],
  ['Руководитель', '______________________________'],
]);

rewriteFooter(2);
rewrite(2, (e) => e.text === 'Актуальность работы', 'Актуальность работы');
rewrite(
  2,
  (e) => e.text?.startsWith('Современные отрасли промышленности'),
  '• Теплообменники применяются в отоплении, HVAC, холодильной технике, энергетике и промышленности.\n\n• Каталоги производителей используют разные наборы характеристик и уровни детализации.\n\n• Ручное сопоставление моделей занимает время и повышает риск ошибки.\n\n• Требуется единая система строгого поиска, сравнения и проверки источников.',
);

rewriteFooter(3);
rewrite(3, (e) => e.text === 'Цель и задачи работы', 'Цель и задачи работы');
rewrite(
  3,
  (e) => e.text?.includes('Целью работы является'),
  'Цель — разработать web-систему ThermoSelect для первичного выбора теплообменных аппаратов.\n\nЗадачи:\n1) проанализировать предметную область и официальные каталоги;\n2) спроектировать нормализованную базу данных;\n3) реализовать авторизацию, каталог, фильтры, карточку и сравнение;\n4) создать административные функции и Excel-выгрузку;\n5) обеспечить безопасность, тестирование и сборку в один JAR.',
);

rewriteFooter(4);
rewrite(4, (e) => e.text === 'Формализованное описание задачи', 'Формализованное описание задачи');
rewrite(
  4,
  (e) => e.text?.startsWith('Входные переменные:'),
  'Входные переменные:\nX = {q, f, m, a, t, p, g, s}\nq — строка поиска;\nf — семейство;\nm — производитель;\na — область применения;\nt, p, g, s — требования по температуре, давлению, расходу и площади.',
);
rewrite(
  4,
  (e) => e.text?.startsWith('Выходные переменные:'),
  'Выходные данные:\nY = {H₁, H₂, …, Hₙ}\nY — упорядоченный список опубликованных аппаратов, которые проходят все строгие ограничения.',
);
rewrite(
  4,
  (e) => e.text?.startsWith('На основе формализованного описания'),
  'Неизвестное обязательное значение исключает запись. После строгой фильтрации активные критерии нормализуются и формируют устойчивый порядок результатов.',
);

rewriteFooter(5);
rewrite(5, (e) => e.text === 'Функциональная структура программного комплекса', 'Функциональная структура программного комплекса');
await addImage(5, 'docs/diagrams/components.png', 'Функциональная структура ThermoSelect', { left: 75, top: 105, width: 810, height: 535 });

rewriteFooter(6);
rewrite(6, (e) => e.text === 'Блок-схема алгоритма работы системы', 'Блок-схема алгоритма подбора');
await replaceImage(
  6,
  (e) => e.name === 'Рисунок 6',
  'docs/presentation/assets/selection-flow-compact.png',
  'Блок-схема фильтрации и ранжирования',
  { frame: { left: 50, top: 105, width: 860, height: 600 }, fit: 'contain' },
);

rewriteFooter(7);
rewrite(7, (e) => e.text?.startsWith('UML-диаграммы прецедентов'), 'UML-диаграмма вариантов использования');
await addImage(7, 'docs/diagrams/use-cases.png', 'UML-варианты использования ThermoSelect', { left: 90, top: 105, width: 780, height: 535 });

rewriteFooter(8);
rewrite(8, (e) => e.text?.startsWith('Концептуальная модель базы данных'), 'Концептуальная модель базы данных');
await addImage(8, 'docs/diagrams/data-model.png', 'Концептуальная модель данных ThermoSelect', { left: 80, top: 105, width: 800, height: 535 });

rewriteFooter(9);
rewrite(9, (e) => e.text?.startsWith('Даталогическая модель базы данных'), 'Даталогическая модель базы данных');
await replaceImage(9, (e) => e.name === 'Рисунок 3', 'docs/diagrams/data-model.png', 'Даталогическая модель данных ThermoSelect');

rewriteFooter(10);
rewrite(10, (e) => e.text?.startsWith('Преимущества и недостатки'), 'Сравнение реляционных СУБД');
rewrite(
  10,
  (e) => e.text?.startsWith('В качестве СУБД выбрана'),
  'PostgreSQL выбрана за ограничения целостности, MVCC и предсказуемую транзакционную модель. H2 используется только для тестов и локального запуска.',
);
setTable(10, [
  ['СУБД', 'Преимущества', 'Вывод'],
  ['PostgreSQL', 'MVCC, ограничения, транзакции, зрелая Java-интеграция', 'Выбрана для эксплуатации'],
  ['MySQL', 'Высокая распространённость, InnoDB, удобное администрирование', 'Допустимая альтернатива'],
  ['SQLite', 'Минимальная установка, один файл', 'Не для многопользовательского сервера'],
]);

rewriteFooter(11);
rewrite(11, (e) => e.text?.startsWith('Преимущества и недостатки'), 'Сравнение технологических стеков');
rewrite(
  11,
  (e) => e.text?.startsWith('Visual Studio выбрана'),
  'React + Spring Boot выбраны за компонентный UI, типизированную серверную модель, Spring Security, интеграционные тесты и поставку в одном исполняемом JAR.',
);
setTable(11, [
  ['Вариант', 'Сильные стороны', 'Ограничения'],
  ['React + Spring Boot', 'Экосистема, Security, JPA, Maven/JAR', 'Требуются Java и JavaScript'],
  ['Vue + NestJS', 'Низкий порог входа, единый TypeScript', 'Отдельная Node.js-поставка'],
  ['Angular + ASP.NET Core', 'Строгая структура, зрелый сервер', 'Тяжелее для срока практики'],
]);

rewriteFooter(12);
rewrite(12, (e) => e.text === 'Структура программного обеспечения', 'Архитектура ThermoSelect');
await addImage(12, 'docs/diagrams/components.png', 'Компонентная архитектура ThermoSelect', { left: 75, top: 105, width: 810, height: 535 });

rewriteFooter(13);
rewrite(13, (e) => e.text === 'Характеристика программного обеспечения', 'Характеристика программного обеспечения');
setTable(13, [
  ['Показатель', 'Значение'],
  ['Интерфейс', 'React 19, Router, адаптивный CSS'],
  ['Сборщик frontend', 'Vite 8'],
  ['Backend', 'Spring Boot 4.1, Java'],
  ['Безопасность', 'Spring Security, CSRF, BCrypt'],
  ['Доступ', 'USER и ADMIN'],
  ['База данных', 'PostgreSQL / файловая H2'],
  ['Миграции', 'Flyway, Hibernate validate'],
  ['Каталог', '50 аппаратов, 4 семейства'],
  ['Поиск', 'Строгая фильтрация и ранжирование'],
  ['Сравнение', 'До 4 моделей'],
  ['Экспорт', 'Excel по фильтрам'],
  ['Администрирование', 'CRUD, статусы, optimistic locking'],
  ['Тестирование', 'JUnit, MockMvc, Vitest'],
  ['Поставка', 'Единый исполняемый JAR'],
  ['Язык интерфейса', 'Русский'],
]);

rewriteFooter(14);
rewrite(14, (e) => e.text === 'Минимальные системные требования', 'Минимальные системные требования');
setTable(14, [
  ['Показатель', 'Значение'],
  ['Процессор', 'x86-64, 2 ядра'],
  ['Оперативная память', '4 ГБ; рекомендуется 8 ГБ'],
  ['Диск', '500 МБ для JAR и локальных данных'],
  ['Операционная система', 'Windows 10/11, Linux или macOS'],
  ['Среда выполнения', 'Java 25 или совместимая версия проекта'],
  ['Клиент', 'Современный браузер с JavaScript'],
]);

rewriteFooter(15);
rewrite(15, (e) => e.text === 'Исходные данные для тестирования программного комплекса', 'Пользовательский сценарий: каталог и карточка');
setTable(15, [
  ['Критерий', 'Пример значения'],
  ['Семейство', 'Пластинчатый'],
  ['Производитель', 'Alfa Laval'],
  ['Применение', 'Отопление'],
  ['Материал', 'Нержавеющая сталь'],
  ['Давление', 'Не менее 10 bar'],
  ['Температура', 'До 180 °C'],
  ['Расход', 'До 6 м³/ч'],
  ['Результат', 'Карточки с источниками и Excel-выгрузкой'],
]);
await replaceImage(15, (e) => e.name === 'Рисунок 11', 'docs/presentation/assets/catalog.png', 'Каталог ThermoSelect', { fit: 'cover' });
await replaceImage(15, (e) => e.name === 'Рисунок 14', 'docs/presentation/assets/detail.png', 'Карточка теплообменника ThermoSelect', { fit: 'cover' });

rewriteFooter(16);
rewrite(16, (e) => e.text === 'Исходные данные для тестирования программного комплекса', 'Администрирование каталога');
rewrite(16, (e) => e.text === 'Добавленные данные', 'Публикация, редактирование и архивирование записей');
await replaceImage(16, (e) => e.name === 'Рисунок 10', 'docs/presentation/assets/admin-catalog.png', 'Административный каталог ThermoSelect', { fit: 'contain' });

rewriteFooter(17);
rewrite(17, (e) => e.text === 'Тестирование программного комплекса', 'Тестирование и Excel-выгрузка');
rewrite(17, (e) => e.text?.startsWith('Сформированный документ'), 'Проверены Java API, роли, CSRF, фильтры, карточки, сравнение, CRUD и Excel-выгрузка');
setTable(17, [
  ['Проверка', 'Результат'],
  ['mvnw.cmd clean verify', 'Java + React tests и исполняемый JAR — успешно'],
]);
await replaceImage(17, (e) => e.name === 'Рисунок 3', 'docs/presentation/assets/catalog.png', 'Excel-выгрузка каталога', { fit: 'cover', crop: { left: 0, top: 0.20, right: 0, bottom: 0.65 } });
await replaceImage(17, (e) => e.name === 'Рисунок 8', 'docs/presentation/assets/admin.png', 'Проверка административной панели', { fit: 'cover', crop: { left: 0, top: 0.08, right: 0, bottom: 0.66 } });
await replaceImage(17, (e) => e.name === 'Рисунок 12', 'docs/presentation/assets/detail.png', 'Проверка карточки аппарата', { fit: 'cover', crop: { left: 0, top: 0.08, right: 0, bottom: 0.60 } });

rewriteFooter(18);
rewrite(18, (e) => e.text === 'Выводы по работе', 'Выводы по работе');
rewrite(
  18,
  (e) => e.text?.startsWith('В ходе выполнения работы'),
  'В результате практики:\n1) разработана нормализованная модель каталога и загружено 50 аппаратов;\n2) реализованы регистрация, вход, каталог, фильтры, карточка, сравнение и Excel;\n3) создана админка со статусами и optimistic locking;\n4) обеспечены CSRF-защита, отзыв доступа и контроль ролей;\n5) Java- и React-тесты объединены с production-сборкой одного JAR.\n\nThermoSelect сокращает первичный поиск, но не подменяет тепловой расчёт.',
);

rewriteFooter(19);
rewrite(19, (e) => e.text === 'Спасибо за внимание!', 'Спасибо за внимание!');

await fs.mkdir(finalSlidesDir, { recursive: true });
await fs.mkdir(finalLayoutDir, { recursive: true });
for (const [index, slide] of presentation.slides.items.entries()) {
  const number = String(index + 1).padStart(2, '0');
  await saveBlobToFile(await presentation.export({ slide, format: 'png', scale: 1.2 }), path.join(finalSlidesDir, `slide-${number}.png`));
  await saveBlobToFile(await presentation.export({ slide, format: 'layout' }), path.join(finalLayoutDir, `slide-${number}.layout.json`));
}

await saveBlobToFile(
  await presentation.export({ format: 'webp', montage: true, scale: 1 }),
  path.join(workspace, 'final-montage.webp'),
);

const finalInspect = await presentation.inspect({
  kind: 'slide,textbox,shape,image,table,chart',
  maxChars: 1_000_000,
});
await fs.writeFile(path.join(workspace, 'final-inspect.ndjson'), finalInspect.ndjson || '', 'utf8');

await fs.mkdir(path.dirname(outputPptx), { recursive: true });
const pptx = await PresentationFile.exportPptx(presentation);
await pptx.save(outputPptx);
console.log(outputPptx);
