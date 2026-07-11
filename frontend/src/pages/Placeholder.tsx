export default function Placeholder({ title }: { title: string }) {
  return (
    <div className="flex items-center justify-center h-full text-slate-500">
      <h2 className="text-2xl font-semibold">{title} Page Coming Soon</h2>
    </div>
  );
}
